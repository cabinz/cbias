package passes.mc.registerAllocation;

import backend.ARMAssemble;
import backend.MCBuilder;
import backend.PrintInfo;
import backend.armCode.MCBasicBlock;
import backend.armCode.MCFPInstruction;
import backend.armCode.MCFunction;
import backend.armCode.MCInstruction;
import backend.armCode.MCInstructions.*;
import backend.operand.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Graph Coloring for Virtual Extension Registers
 */
public class GC4VER {

    /**
     * Color, also the number of register.<br/>
     * In ARM, we use s0-s31 for float
     * @see backend.operand.RealExtRegister
     */
    private int K = 32;

    //<editor-fold desc="Data Structure">
    //<editor-fold desc="Key worklist set">
    /**
     * The set of low-degree non-move-related nodes
     */
    private HashSet<ExtensionRegister> simplifyWorklist;
    /**
     * The set of move instructions that might be coalescing
     */
    private HashSet<MCFPmove> worklistMoves;
    /**
     * The set of low-degree move-related nodes
     */
    private HashSet<ExtensionRegister> freezeWorklist;
    /**
     * The set of high-degree nodes
     */
    private HashSet<ExtensionRegister> spillWorklist;
    //</editor-fold>

    //<editor-fold desc="Register Interfere Graph">
    /**
     * Adjacency list representation of the graph. <br/>
     * For each non-precolored temporary <i>u</i>, adjList[<i>u</i> ] is
     * the set of nodes that interfere with <i>u</i>
     */
    private HashMap<ExtensionRegister, HashSet<ExtensionRegister>> adjList;
    /**
     * The set of interference edge (<i>u</i>, <i>v</i> ) in the graph. <br/>
     * If (<i>u</i>, <i>v</i> ) ∈ adjSet, then (<i>v</i>, <i>u</i> ) ∈ adjSet
     */
    private HashSet<Pair<ExtensionRegister, ExtensionRegister>> adjSet;
    /**
     * an array containing the current degree of each node
     */
    HashMap<ExtensionRegister, Integer> degree;
    //</editor-fold>

    //<editor-fold desc="Coloring Info">
    /**
     * Stack containing temporaries removed from the graph
     */
    private Stack<ExtensionRegister> selectStack;
    /**
     * As its name
     */
    private HashSet<ExtensionRegister> coloredNodes;
    /**
     * The color chosen by algorithm for a node. <br/>
     * For the precolored nodes, this is initialized to the given color.
     */
    private HashMap<ExtensionRegister, Integer> color;
    //</editor-fold>

    //<editor-fold desc="Node Info">
    /**
     * The registers that have been coalesced;
     * when <i>u</i> &#8592;<i>v</i> is coalesced, <i>v</i> is added to this set
     * and <i>u</i> put back on some worklist (or vice versa)
     */
    private HashSet<ExtensionRegister> coalescedNodes;
    /**
     * When a move (<i>u</i>, <i>v</i> ) has been coalesced,
     * and <i>v</i> put in coalescedNodes, then alias(<i>v</i> ) = <i>u</i>
     */
    private HashMap<ExtensionRegister, ExtensionRegister> alias;
    /**
     * The nodes marked for spilling during this round; initially empty;
     */
    private HashSet<ExtensionRegister> spilledNodes;
    //</editor-fold>

    //<editor-fold desc="Move Info">
    /**
     * a mapping from a node to the list of moves it is associated with
     */
    private HashMap<ExtensionRegister, HashSet<MCFPmove>> moveList;
    /**
     * Moves not yet ready for coalescing
     */
    private HashSet<MCFPmove> activeMoves;
    /**
     * Moves that have been coalesced
     */
    private HashSet<MCFPmove> coalescedMoves;
    /**
     * Moves whose source and target interfere
     */
    private HashSet<MCFPmove> constrainedMoves;
    /**
     * Moves that will no longer be considered for coalescing
     */
    private HashSet<MCFPmove> frozenMove;
    //</editor-fold>

    //<editor-fold desc="Tools">
    private MCFunction curFunc;
    private HashMap<MCBasicBlock, LiveInfo> liveInfo;
    private HashMap<MCOperand, Integer> liveRange;
    private final int INF = 0x3F3F3F3F;
    private HashSet<Integer> usedColor;
    //</editor-fold>
    //</editor-fold>

    public void runOnModule(ARMAssemble armAssemble) {
        /* main produce of the Graph Coloring */
        for (MCFunction func : armAssemble){
            if (func.isExternal())
                continue;
            curFunc = func;

            //<editor-fold desc="Graph Coloring">
            while (true) {
                Initialize();
                liveInfo = LivenessAnalysis.run(func);
                liveRange = LivenessAnalysis.liveRangeAnalysis(func);
                Build();
                MakeWorklist();
                do {
                    if (!simplifyWorklist.isEmpty()) Simplify();
                    else if (!worklistMoves.isEmpty()) Coalesce();
                    else if (!freezeWorklist.isEmpty()) Freeze();
                    else if (!spillWorklist.isEmpty()) SelectSpill();
                } while (!(simplifyWorklist.isEmpty() && worklistMoves.isEmpty() && freezeWorklist.isEmpty() && spillWorklist.isEmpty()));
                AssignColors();
                if (spilledNodes.isEmpty())
                    break;
                else
                    RewriteProgram();
            }
            //</editor-fold>

            //<editor-fold desc="Register Allocation">
            ReplaceRegisters();

            if (!PrintInfo.printIR)
            RemoveCoalesced();

            /* Fix function stack */
            usedColor.forEach(func::addExtContext);

            AdjustParamLoad();

            PreserveContext();
            //</editor-fold>
        }
    }

    //<editor-fold desc="Main procedure function">
    /**
     * Initialize the context
     */
    private void Initialize() {
        simplifyWorklist = new HashSet<>();
        worklistMoves = new HashSet<>();
        freezeWorklist = new HashSet<>();
        spillWorklist = new HashSet<>();

        adjList = new HashMap<>();
        adjSet = new HashSet<>();
        degree = new HashMap<>(IntStream.range(0, K)
                .mapToObj(RealExtRegister::get)
                .collect(Collectors.toMap(x -> x, x -> INF)));

        selectStack = new Stack<>();
        coloredNodes = IntStream.range(0, K)
                .mapToObj(RealExtRegister::get)
                .collect(Collectors.toCollection(HashSet::new));
        color = new HashMap<>(IntStream.range(0, K)
                .mapToObj(RealExtRegister::get)
                .collect(Collectors.toMap(x -> x, RealExtRegister::getIndex)));
        coalescedNodes = new HashSet<>();
        alias = new HashMap<>();
        spilledNodes = new HashSet<>();

        moveList = new HashMap<>();
        activeMoves = new HashSet<>();

        coalescedMoves = new HashSet<>();
        constrainedMoves = new HashSet<>();
        frozenMove = new HashSet<>();

        usedColor = new HashSet<>();
    }

    /**
     * Build the RIG &amp; moveList
     */
    private void Build() {
        for (var block : curFunc) {
            var live = new HashSet<>(liveInfo.get(block).extOut);
            for (int i=block.getInstructionList().size()-1; i>=0; i--) {
                MCInstruction instruction = block.getInstructionList().get(i);
                if (instruction instanceof MCbranch) {
                    var br = ((MCbranch) instruction);
                    var uses = br.getExtUse();
                    var defs = br.getExtDef();

                    /* live = live ∪ def */
                    live.addAll(defs);

                    /* Build the RIG */
                    /* Add an edge from def to each node in live */
                    defs.forEach(d -> live.forEach(l -> AddEdge(l, d)));

                    /* live = uses ∪ live\defs) */
                    live.removeAll(defs);
                    live.addAll(uses);
                }
                if (!(instruction instanceof MCFPInstruction)) continue;
                var inst = (MCFPInstruction) instruction;
                var uses = inst.getExtUse();
                var defs = inst.getExtDef();

                /* Copy will be coalescing */
                if (inst instanceof MCFPmove && inst.getCond() == null && ((MCFPmove) inst).isCopy()) {
                    var move = (MCFPmove) inst;

                    /* Delete the variable in live */
                    live.removeAll(uses);

                    /* Add this to moveList */
                    uses.forEach(use -> moveList.putIfAbsent(use, new HashSet<>()));
                    uses.forEach(use -> moveList.get(use).add(move));

                    defs.forEach(def -> moveList.putIfAbsent(def, new HashSet<>()));
                    defs.forEach(def -> moveList.get(def).add(move));

                    /* Add to be coalescing */
                    worklistMoves.add(move);
                }
                /* live = live ∪ def */
                live.addAll(defs);

                /* Build the RIG */
                /* Add an edge from def to each node in live */
                defs.forEach(d -> live.forEach(l -> AddEdge(l, d)));

                /* live = uses ∪ live\defs) */
                live.removeAll(defs);
                live.addAll(uses);
            }
        }
    }

    /**
     * Fill the worklist
     */
    private void MakeWorklist() {
        curFunc.getVirtualExtRegisters().forEach(n -> {
            if (degree.getOrDefault(n, 0) >= K)
                spillWorklist.add(n);
            else if (MoveRelated(n))
                freezeWorklist.add(n);
            else
                simplifyWorklist.add(n);
        });
    }

    /**
     * Simplify the RIG by removing the simplifyWorklist and push into selectStack
     */
    private void Simplify() {
        ExtensionRegister n = simplifyWorklist.iterator().next();
        simplifyWorklist.remove(n);
        selectStack.push(n);
        Adjacent(n).forEach(this::DecrementDegree);
    }

    /**
     * Coalesce copy instruction
     */
    private void Coalesce() {
        MCFPmove m = worklistMoves.iterator().next();
        worklistMoves.remove(m);

        ExtensionRegister x = GetAlias((ExtensionRegister) m.getDst1());
        ExtensionRegister y = GetAlias((ExtensionRegister) m.getSrc1());

        ExtensionRegister u, v;
        if (isPrecolored(y)) {
            u = y;
            v = x;
        }
        else {
            u = x;
            v = y;
        }

        if (u == v) {
            coalescedMoves.add(m);
            AddWorkList(u);
        }
        else if (isPrecolored(v) || adjSet.contains(new Pair<>(u, v))) {
            constrainedMoves.add(m);
            AddWorkList(u);
            AddWorkList(v);
        }
        else if ((isPrecolored(u) && OK(Adjacent(v), u)) || (!isPrecolored(u) && Conservative(Adjacent(u), Adjacent(v)))) {
            coalescedMoves.add(m);
            Combine(u, v);
            AddWorkList(u);
        }
        else {
            activeMoves.add(m);
        }
    }

    /**
     * Freeze the copy to keep simplifying
     */
    private void Freeze() {
        var u = freezeWorklist.iterator().next();
        freezeWorklist.remove(u);
        simplifyWorklist.add(u);
        FreezeMoves(u);
    }

    /**
     * Select a node to be spilled, but not real spilled. <br/>
     * Spilling is postponed after AssignColor, because the spilled node may be able to be colored. <br/>
     * Random select now, be to optimized later ...
     */
    private void SelectSpill() {
        var m = spillWorklist.stream()
                .max(Comparator.comparingInt(liveRange::get))
                .get();
        spillWorklist.remove(m);
        simplifyWorklist.add(m);
        FreezeMoves(m);
    }

    /**
     * Assign color to all nodes, trying to assign the spilled node.
     * If the color available is empty, the node must be spilled.
     */
    private void AssignColors() {
        while (!selectStack.isEmpty()) {
            /* Initialize */
            var n = selectStack.pop();
            var okColor = IntStream.range(0, K).boxed()
                    .collect(Collectors.toList());

            /* Remove unavailable color */
            if (adjList.containsKey(n))
                adjList.getOrDefault(n, new HashSet<>()).forEach(w -> {
                    if (isPrecolored(GetAlias(w)) || coloredNodes.contains(GetAlias(w)))
                        okColor.remove(color.get(GetAlias(w)));
                });

            /* Assign */
            if (okColor.isEmpty())
                spilledNodes.add(n);
            else {
                coloredNodes.add(n);
                color.put(n, okColor.get(0));
                usedColor.add(okColor.get(0));
            }
        }

        /* Deal coalesced nodes */
        coalescedNodes.forEach(n -> color.put(n, color.get(GetAlias(n))));
    }


    private void RewriteProgram() {
        for (var v : spilledNodes) {
            int offset = curFunc.getStackSize();

            for (var block : curFunc) {
                MCFPInstruction lastDef = null;
                boolean firstUse = true;
                int counter = 0;

                ExtensionRegister v_tmp = curFunc.createExtVirReg(((VirtualExtRegister) v).getValue());

                var list = block.getInstructionList();
                for (int i=0; i<list.size(); i++) {
                    var curInst = list.get(i);
                    counter ++;

                    if (curInst instanceof MCFPInstruction) {
                        var inst = ((MCFPInstruction) curInst);
                        /* If use, load from memory */
                        if (inst.getExtUse().contains(v)) {
                            if (lastDef == null && firstUse) {
                                if (1020 > offset && offset > -1020)
                                    inst.insertBefore(new MCFPload(v_tmp, RealRegister.get(13), new Immediate(offset)));
                                else {
                                    VirtualRegister addr = curFunc.createVirReg(offset);
                                    inst.insertBefore(new MCMove(addr, new Immediate(offset), true));
                                    inst.insertBefore(new MCBinary(MCInstruction.TYPE.ADD, addr, RealRegister.get(13), addr));
                                    inst.insertBefore(new MCFPload(v_tmp, addr));
                                    i++;
                                }

                                firstUse = false;
                                i++;
                            }

                            inst.replaceExtReg(v, v_tmp);
                        }

                        /* If def, store the def into memory */
                        if (inst.getExtDef().contains(v)) {
                            lastDef = inst;
                            inst.replaceExtReg(v, v_tmp);
                        }

                        if (counter >= 30) {
                            if (lastDef != null) {
                                if (1020 > offset && offset > -1020)
                                    lastDef.insertAfter(new MCFPstore(v_tmp, RealRegister.get(13), new Immediate(offset)));
                                else {
                                    VirtualRegister addr = curFunc.createVirReg(offset);
                                    lastDef.insertAfter(new MCFPstore(v_tmp, addr));
                                    lastDef.insertAfter(new MCBinary(MCInstruction.TYPE.ADD, addr, RealRegister.get(13), addr));
                                    lastDef.insertAfter(new MCMove(addr, new Immediate(offset), true));
                                    i++;
                                }
                                lastDef = null;
                                i++;
                            }

                            counter = 0;
                            firstUse = true;
                            v_tmp = curFunc.createExtVirReg(((VirtualExtRegister) v).getValue());
                        }
                    }
                }
                if (lastDef != null) {
                    if (1020 > offset && offset > -1020)
                        lastDef.insertAfter(new MCFPstore(v_tmp, RealRegister.get(13), new Immediate(offset)));
                    else {
                        VirtualRegister addr = curFunc.createVirReg(offset);
                        lastDef.insertAfter(new MCFPstore(v_tmp, addr));
                        lastDef.insertAfter(new MCBinary(MCInstruction.TYPE.ADD, addr, RealRegister.get(13), addr));
                        lastDef.insertAfter(new MCMove(addr, new Immediate(offset), true));
                    }
                }
            }

            curFunc.addSpilledNode(v);
        }
    }

    private void ReplaceRegisters() {
        curFunc.forEach(block -> block.forEach(inst -> {
            if (inst instanceof MCFPInstruction)
                assignRealRegister((MCFPInstruction) inst);
        }));
    }

    private void RemoveCoalesced() {
        coalescedMoves.forEach(MCInstruction::removeSelf);
    }

    /**
     * Add the context and spilled nodes' space to the parameter loads' offset
     */
    private void AdjustParamLoad() {
        curFunc.getParamCal().forEach(move -> {
            int new_offset =
                    ((Immediate) move.getSrc()).getIntValue()
                    + curFunc.getExtContext().size() * 4;
            move.setSrc(new Immediate(new_offset));
            if (!MCBuilder.canEncodeImm(new_offset))
                move.setExceededLimit();
        });
    }

    /**
     * Add PUSH in the front of procedure to preserve the context if necessary
     */
    private void PreserveContext() {
        if (!curFunc.getExtContext().isEmpty())
            curFunc.getEntryBlock().prependInst(new MCFPpush(curFunc.getExtContext()));
    }
    //</editor-fold>

    //<editor-fold desc="Tool method">
    /**
     * Add an edge in the RIG
     * @param u node 1
     * @param v node 2
     */
    private void AddEdge(ExtensionRegister u, ExtensionRegister v) {
        if (!adjSet.contains(new Pair<>(u, v)) && !u.equals(v)) {
            /* Build edge */
            adjSet.add(new Pair<>(u, v));
            adjSet.add(new Pair<>(v, u));

            /* Add v to the adjList(u) */
            if (!isPrecolored(u)) {
                adjList.putIfAbsent(u, new HashSet<>());
                adjList.get(u).add(v);

                /* Adjust degree */
                degree.compute(u, (key, value) -> value==null ?0 :value+1);
            }
            /* Add u to the adjList(v) */
            if (!isPrecolored(v)) {
                adjList.putIfAbsent(v, new HashSet<>());
                adjList.get(v).add(u);

                /* Adjust degree */
                degree.compute(v, (key, value) -> value==null ?0 :value+1);
            }
        }
    }

    /**
     * Decrement the degree of one node
     * @param m the node to be operated
     */
    private void DecrementDegree(ExtensionRegister m) {
        Integer d = degree.get(m);
        degree.put(m, d-1);

        /* If go out from spillNode */
        if (d == K) {
            EnableMoves(m);
            Adjacent(m).forEach(this::EnableMoves);
            spillWorklist.remove(m);

            if (MoveRelated(m))
                freezeWorklist.add(m);
            else
                simplifyWorklist.add(m);
        }
    }

    /**
     * Enable the move to be coalesced
     * @param n the node to be operated
     */
    private void EnableMoves(ExtensionRegister n) {
        NodeMoves(n).forEach(m -> {
            if (activeMoves.contains(m)) {
                activeMoves.remove(m);
                worklistMoves.add(m);
            }
        });
    }

    /**
     * Determine whether the node is move-related
     */
    private boolean MoveRelated(ExtensionRegister n) {
        return !NodeMoves(n).isEmpty();
    }

    /**
     * All the MOVE instruction related with n
     * @param n the node to find
     * @return the set of MOVE
     */
    private Set<MCFPmove> NodeMoves(ExtensionRegister n) {
        return moveList.getOrDefault(n, new HashSet<>()).stream()
                .filter(move -> activeMoves.contains(move) || worklistMoves.contains(move))
                .collect(Collectors.toSet());
    }

    /**
     * Find all the node adjacent to n
     * @param n the node to find
     * @return the set of neighbor
     */
    private Set<ExtensionRegister> Adjacent(ExtensionRegister n) {
        return adjList.getOrDefault(n, new HashSet<>()).stream()
                .filter(node -> !selectStack.contains(node) && !coalescedNodes.contains(node))
                .collect(Collectors.toSet());
    }

    /**
     * Make one node to be simplified
     */
    private void AddWorkList(ExtensionRegister u) {
        if (!isPrecolored(u) && !MoveRelated(u) && degree.getOrDefault(u, 0)<K) {
            freezeWorklist.remove(u);
            simplifyWorklist.add(u);
        }
    }

    /**
     * Get alias of a node.
     */
    private ExtensionRegister GetAlias(ExtensionRegister n) {
        while (coalescedNodes.contains(n))
            n = alias.get(n);
        return n;
    }

    /**
     * Test the Adjacent is OK for coalescing <br/>
     * George’s Algorithm
     */
    private boolean OK(Set<ExtensionRegister> ts, ExtensionRegister r) {
        return ts.stream().allMatch(t -> OK(t, r));
    }

    /**
     * Test two node is OK for coalescing <br/>
     * George’s Algorithm
     */
    private boolean OK(ExtensionRegister t, ExtensionRegister r) {
        return degree.get(t) < K || isPrecolored(t) || adjSet.contains(new Pair<>(t, r));
    }

    /**
     * Test the coalescing using conservative way <br/>
     * Briggs's Algorithm
     */
    private boolean Conservative(Set<ExtensionRegister> nodes, Set<ExtensionRegister> v) {
        nodes.addAll(v);
        AtomicInteger k = new AtomicInteger();
        nodes.forEach(n -> {
            if (degree.get(n) >= K) k.getAndIncrement();
        });
        return k.get() < K;
    }

    /**
     * Combine <i>v</i>  to <i>u</i> <br/>
     * <i>u</i> &#8592;<i>v</i>
     * @param u the node combine to
     * @param v the node to be combined
     */
    private void Combine(ExtensionRegister u,ExtensionRegister v) {
        if (freezeWorklist.contains(v))
            freezeWorklist.remove(v);
        else
            spillWorklist.remove(v);

        /* Combine this two */
        coalescedNodes.add(v);
        alias.put(v, u);
        moveList.get(u).addAll(moveList.get(v));

        /* Adjust RIG */
        Adjacent(v).forEach(t -> {
            AddEdge(t, u);
            DecrementDegree(t);
        });

        if (degree.getOrDefault(u, 0) >= K && freezeWorklist.contains(u)) {
            freezeWorklist.remove(u);
            spillWorklist.add(u);
        }
    }

    private void FreezeMoves(ExtensionRegister u) {
        NodeMoves(u).forEach(move -> {
            var v = GetAlias(((ExtensionRegister) move.getSrc1())) == GetAlias(u)
                    ?GetAlias((ExtensionRegister) move.getDst1())
                    :GetAlias((ExtensionRegister) move.getSrc1());

            activeMoves.remove(move);
            frozenMove.add(move);
            if (NodeMoves(v).isEmpty() && degree.get(v) < K) {
                freezeWorklist.remove(v);
                simplifyWorklist.add(v);
            }
        });
    }

    private boolean isPrecolored(ExtensionRegister r) {
        return r instanceof RealExtRegister;
    }

    /**
     * Replace the register in the MCInstruction. <br/>
     * This is ugly, but I have no way, with the bad definition of ARM assemble. <br/>
     * It should be like IR using the user to hold operands, using constructor <br/>
     * to restrict the operand.
     */
    private void assignRealRegister(MCFPInstruction inst) {
        if (inst instanceof MCFPBinary) {
            var bi = ((MCFPBinary) inst);
            replace(bi, bi.getDestination());
            replace(bi, bi.getOperand1());
            replace(bi, bi.getOperand2());
        }
        else if (inst instanceof MCFPcompare) {
            var cmp = ((MCFPcompare) inst);
            replace(cmp, cmp.getOperand1());
            replace(cmp, cmp.getOperand2());
        }
        else if (inst instanceof MCFPconvert) {
            var cvt = ((MCFPconvert) inst);
            replace(cvt, cvt.getSrc());
            replace(cvt, cvt.getDst());
        }
        else if (inst instanceof MCFPneg) {
            var neg = ((MCFPneg) inst);
            replace(neg, neg.getOperand());
            replace(neg, neg.getDestination());
        }
        else if (inst instanceof MCFPload) {
            var load = ((MCFPload) inst);
            replace(load, load.getDst());
        }
        else if (inst instanceof MCFPstore) {
            var store = ((MCFPstore) inst);
            replace(store, store.getSrc());
        }
        else if (inst instanceof MCFPmove) {
            var mov = ((MCFPmove) inst);
            var src1 = mov.getSrc1();
            var dst1 = mov.getDst1();
            if (src1 != null && src1.isVirtualExtReg())
                replace(mov, (ExtensionRegister) src1);
            if (dst1 != null && dst1.isVirtualExtReg())
                replace(mov, (ExtensionRegister) dst1);
        }

    }

    private void replace(MCFPInstruction inst, ExtensionRegister old) {
        inst.replaceExtReg(old, RealExtRegister.get(color.get(old)));
    }
    //</editor-fold>
}
