package passes.mc.RegisterAllocation;

import backend.ARMAssemble;
import backend.MCBuilder;
import backend.armCode.MCBasicBlock;
import backend.armCode.MCFunction;
import backend.armCode.MCInstruction;
import backend.armCode.MCInstructions.*;
import backend.operand.Immediate;
import backend.operand.RealRegister;
import backend.operand.Register;
import backend.operand.VirtualRegister;
import passes.mc.MCPass;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Implement using George's Iterated Register Coalescing Graph Coloring.<br/>
 * @see <a href='https://zh.hu1lib.vip/book/3715805/6a4e7d'>Modern Compiler Implementation in C</a> Charpter 11
 * @see <a href='http://underpop.online.fr/j/java/help/graph-coloring-implementation-compiler-java-programming-language.html.gz'>
 *     Graph-Coloring implement</a>
 */
public class GraphColoring implements MCPass {

    /**
     * Color, also the number of register.<br/>
     * In ARM, we use r0-r12
     * @see backend.operand.RealRegister
     */
    private int K = 13;

    //<editor-fold desc="Data Structure">
    //<editor-fold desc="Key worklist set">
    /**
     * The set of low-degree non-move-related nodes
     */
    private HashSet<Register> simplifyWorklist;
    /**
     * The set of move instructions that might be coalescing
     */
    private HashSet<MCMove> worklistMoves;
    /**
     * The set of low-degree move-related nodes
     */
    private HashSet<Register> freezeWorklist;
    /**
     * The set of high-degree nodes
     */
    private HashSet<Register> spillWorklist;
    //</editor-fold>

    //<editor-fold desc="Register Interfere Graph">
    /**
     * Adjacency list representation of the graph. <br/>
     * For each non-precolored temporary <i>u</i>, adjList[<i>u</i> ] is
     * the set of nodes that interfere with <i>u</i>
     */
    private HashMap<Register, HashSet<Register>> adjList;
    /**
     * The set of interference edge (<i>u</i>, <i>v</i> ) in the graph. <br/>
     * If (<i>u</i>, <i>v</i> ) ∈ adjSet, then (<i>v</i>, <i>u</i> ) ∈ adjSet
     */
    private HashSet<Pair<Register, Register>> adjSet;
    /**
     * an array containing the current degree of each node
     */
    HashMap<Register, Integer> degree;
    //</editor-fold>

    //<editor-fold desc="Coloring Info">
    /**
     * Stack containing temporaries removed from the graph
     */
    private Stack<Register> selectStack;
    /**
     * As its name
     */
    private HashSet<Register> coloredNodes;
    /**
     * The color chosen by algorithm for a node. <br/>
     * For the precolored nodes, this is initialized to the given color.
     */
    private HashMap<Register, Integer> color;
    //</editor-fold>

    //<editor-fold desc="Node Info">
    /**
     * The registers that have been coalesced;
     * when <i>u</i> &#8592<i>v</i> is coalesced, <i>v</i> is added to this set
     * and <i>u</i> put back on some worklist (or vice versa)
     */
    private HashSet<Register> coalescedNodes;
    /**
     * When a move (<i>u</i>, <i>v</i> ) has been coalesced,
     * and <i>v</i> put in coalescedNodes, then alias(<i>v</i> ) = <i>u</i>
     */
    private HashMap<Register, Register> alias;
    /**
     * The nodes marked for spilling during this round; initially empty;
     */
    private HashSet<Register> spilledNodes;
    //</editor-fold>

    //<editor-fold desc="Move Info">
    /**
     * a mapping from a node to the list of moves it is associated with
     */
    private HashMap<Register, HashSet<MCMove>> moveList;
    /**
     * Moves not yet ready for coalescing
     */
    private HashSet<MCMove> activeMoves;
    /**
     * Moves that have been coalesced
     */
    private HashSet<MCMove> coalescedMoves;
    /**
     * Moves whose source and target interfere
     */
    private HashSet<MCMove> constrainedMoves;
    /**
     * Moves that will no longer be considered for coalescing
     */
    private HashSet<MCMove> frozenMove;
    //</editor-fold>

    //<editor-fold desc="Tools">
    private MCFunction curFunc;
    private HashMap<MCBasicBlock, LiveInfo> liveInfo;
    private final int INF = 0x3F3F3F3F;
    private HashSet<Integer> usedColor;
    //</editor-fold>
    //</editor-fold>

    @Override
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

            RemoveCoalesced();

            /* Fix function stack */
            usedColor.forEach(func::addContext);

            AdjustParamLoad();

            AdjustStack();

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
        degree = new HashMap<>(IntStream.range(0, 16)
                .mapToObj(RealRegister::get)
                .collect(Collectors.toMap(x -> x, x -> INF)));

        selectStack = new Stack<>();
        coloredNodes = IntStream.range(0, 16)
                .mapToObj(RealRegister::get).collect(Collectors.toCollection(HashSet::new));
        color = new HashMap<>(IntStream.range(0, 16)
                .mapToObj(RealRegister::get)
                .collect(Collectors.toMap(x -> x, RealRegister::getIndex)));
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
     * Build the RIG & moveList
     */
    private void Build() {
        for (var block : curFunc) {
            var live = new HashSet<>(liveInfo.get(block).out);
            for (int i=block.getInstructionList().size()-1; i>=0; i--) {
                MCInstruction inst = block.getInstructionList().get(i);
                var uses = inst.getUse();
                var defs = inst.getDef();

                /* Copy will be coalescing */
                if (inst instanceof MCMove && inst.getShift() == null && inst.getCond() == null && ((MCMove) inst).isCopy()) {
                    var move = ((MCMove) inst);

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
        curFunc.getVirtualRegisters().forEach(n -> {
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
        Register n = simplifyWorklist.iterator().next();
        simplifyWorklist.remove(n);
        selectStack.push(n);
        Adjacent(n).forEach(this::DecrementDegree);
    }

    /**
     * Coalesce copy instruction
     */
    private void Coalesce() {
        MCMove m = worklistMoves.iterator().next();
        worklistMoves.remove(m);

        Register x = GetAlias(m.getDst());
        Register y = GetAlias(((Register) m.getSrc()));

        Register u, v;
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
        // TODO: BETTER WAY
        var m = spillWorklist.iterator().next();
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
            var okColor = IntStream.range(0, 12).boxed()// TODO: 考虑使用r14 lr? 使用r12 ip
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

    /**
     * Spilling using store after def and load before use. <br/>
     * That's ugly, but useful and simple.
     */
    private void RewriteProgram() {
        for (var v : spilledNodes) {
            int offset = curFunc.getStackSize();

            for (var block : curFunc) {
                var list = block.getInstructionList();
                for (int i=0; i<list.size(); i++) {
                    var inst = list.get(i);

                    /* If def, store the def into memory */
                    if (inst.getDef().contains(v)) {
                        /* The max size can the offset can be, see {@link ARMARMv7}  A8.6.58 Page: A8-121 */
                        if (4096 > offset && offset > -4096)
                            inst.insertAfter(new MCstore(v, RealRegister.get(13), new Immediate(offset)));
                        else {
                            VirtualRegister tmp = curFunc.createVirReg(offset);
                            inst.insertAfter(new MCstore(v, RealRegister.get(13), tmp));
                            inst.insertAfter(new MCMove(tmp, new Immediate(offset), !MCBuilder.canEncodeImm(offset)));
                            i++;
                        }
                        i++;
                    }

                    /* If use, load from memory */
                    if (inst.getUse().contains(v)) {
                        // TODO: 更好的方法？
                        /* Create a temporary v_tmp for the use */
                        VirtualRegister v_tmp = curFunc.createVirReg(((VirtualRegister) v).getValue());
                        if (4096 > offset && offset > -4096)
                            inst.insertBefore(new MCload(v_tmp, RealRegister.get(13), new Immediate(offset)));
                        else {
                            VirtualRegister tmp = curFunc.createVirReg(offset);
                            inst.insertBefore(new MCMove(tmp, new Immediate(offset), !MCBuilder.canEncodeImm(offset)));
                            inst.insertBefore(new MCload(v_tmp, RealRegister.get(13), tmp));
                            i++;
                        }
                        inst.replaceRegister(v, v_tmp);
                        i++;
                    }
                }
            }

            curFunc.addSpilledNode();
        }
    }

    private void ReplaceRegisters() {
        curFunc.forEach(block -> block.forEach(this::assignRealRegister));
    }

    private void RemoveCoalesced() {
        coalescedMoves.forEach(MCInstruction::removeSelf);
    }

    /**
     * Add the context and spilled nodes' space to the parameter loads' offset
     */
    private void AdjustParamLoad() {
        // TODO: 超过限制时需要生成move
        curFunc.getParamCal().forEach(load -> load.setOffset(new Immediate(
                ((Immediate) load.getOffset()).getIntValue()
                        + curFunc.getContext().size() * 4
                        + curFunc.getSpilledNode() * 4
        )));
    }

    /**
     * Add the spilled nodes' space to the function stack
     */
    private void AdjustStack() {
        MCInstruction allocate = curFunc.getEntryBlock().getFirstInst();
        if (allocate instanceof MCBinary) {
            var bi = ((MCBinary) allocate);
            int new_offset = ((Immediate) bi.getOperand2()).getIntValue() + curFunc.getSpilledNode() * 4;
            if (MCBuilder.canEncodeImm(new_offset))
                bi.setOperand2(new Immediate(new_offset));
            else {
                bi.insertBefore(new MCMove(RealRegister.get(4), new Immediate(new_offset), true));
                bi.setOperand2(RealRegister.get(4));
                curFunc.addContext(4);
            }
        }
        else if (allocate instanceof MCMove) {
            var mov = ((MCMove) allocate);
            int new_offset = ((Immediate) mov.getSrc()).getIntValue() + curFunc.getSpilledNode() * 4;
            if (MCBuilder.canEncodeImm(new_offset))
                mov.setSrc(new Immediate(new_offset));
            else {
                mov.setSrc(new Immediate(new_offset));
                mov.setExceededLimit();
            }
        }
    }

    /**
     * Add PUSH in the front of procedure to preserve the context if necessary
     */
    private void PreserveContext() {
        if (!curFunc.getContext().isEmpty())
            curFunc.getEntryBlock().prependInst(new MCpush(curFunc.getContext()));
    }
    //</editor-fold>

    //<editor-fold desc="Tool method">
    /**
     * Add an edge in the RIG
     * @param u node 1
     * @param v node 2
     */
    private void AddEdge(Register u, Register v) {
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
    private void DecrementDegree(Register m) {
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
    private void EnableMoves(Register n) {
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
    private boolean MoveRelated(Register n) {
        return !NodeMoves(n).isEmpty();
    }

    /**
     * All the MOVE instruction related with n
     * @param n the node to find
     * @return the set of MOVE
     */
    private Set<MCMove> NodeMoves(Register n) {
        return moveList.getOrDefault(n, new HashSet<>()).stream()
                .filter(move -> activeMoves.contains(move) || worklistMoves.contains(move))
                .collect(Collectors.toSet());
    }

    /**
     * Find all the node adjacent to n
     * @param n the node to find
     * @return the set of neighbor
     */
    private Set<Register> Adjacent(Register n) {
        return adjList.getOrDefault(n, new HashSet<>()).stream()
                .filter(node -> !selectStack.contains(node) && !coalescedNodes.contains(node))
                .collect(Collectors.toSet());
    }

    /**
     * Make one node to be simplified
     */
    private void AddWorkList(Register u) {
        if (!isPrecolored(u) && !MoveRelated(u) && degree.getOrDefault(u, 0)<K) {
            freezeWorklist.remove(u);
            simplifyWorklist.add(u);
        }
    }

    /**
     * Get alias of a node.
     */
    private Register GetAlias(Register n) {
        while (coalescedNodes.contains(n))
            n = alias.get(n);
        return n;
    }

    /**
     * Test the Adjacent is OK for coalescing <br/>
     * George’s Algorithm
     */
    private boolean OK(Set<Register> ts, Register r) {
        return ts.stream().allMatch(t -> OK(t, r));
    }

    /**
     * Test two node is OK for coalescing <br/>
     * George’s Algorithm
     */
    private boolean OK(Register t, Register r) {
        return degree.get(t) < K || isPrecolored(t) || adjSet.contains(new Pair<>(t, r));
    }

    /**
     * Test the coalescing using conservative way <br/>
     * Briggs's Algorithm
     */
    private boolean Conservative(Set<Register> nodes, Set<Register> v) {
        nodes.addAll(v);
        AtomicInteger k = new AtomicInteger();
        nodes.forEach(n -> {
            if (degree.get(n) >= K) k.getAndIncrement();
        });
        return k.get() < K;
    }

    /**
     * Combine <i>v</i>  to <i>u</i> <br/>
     * <i>u</i> &#8592<i>v</i>
     * @param u the node combine to
     * @param v the node to be combined
     */
    private void Combine(Register u,Register v) {
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

    private void FreezeMoves(Register u) {
         NodeMoves(u).forEach(move -> {
             var v = GetAlias(((Register) move.getSrc())) == GetAlias(u) ?GetAlias(move.getDst()) :GetAlias(((Register) move.getSrc()));

             activeMoves.remove(move);
             frozenMove.add(move);
             if (NodeMoves(v).isEmpty() && degree.get(v) < K) {
                 freezeWorklist.remove(v);
                 simplifyWorklist.add(v);
             }
         });
    }

    private boolean isPrecolored(Register r) {
        return r instanceof RealRegister;
    }

    /**
     * Replace the register in the MCInstruction. <br/>
     * This is ugly, but I have no way, with the bad definition of ARM assemble. <br/>
     * It should be like IR using the user to hold operands, using constructor <br/>
     * to restrict the operand.
     */
    private void assignRealRegister(MCInstruction inst) {
        if (inst instanceof MCBinary) {
            var bi = ((MCBinary) inst);
            var op1 = bi.getOperand1();
            var op2 = bi.getOperand2();
            var dst = bi.getDestination();
            var shift = bi.getShift()==null ?null :bi.getShift().getOperand();
            replace(bi, op1);
            if (op2 instanceof VirtualRegister)
                replace(bi, ((VirtualRegister) op2));
            if (shift instanceof VirtualRegister)
                replace(bi, ((VirtualRegister) shift));
            replace(bi, dst);
        }
        else if (inst instanceof MCcmp) {
            var cmp = ((MCcmp) inst);
            var op1 = cmp.getOperand1();
            var op2 = cmp.getOperand2();
            var shift = cmp.getShift()==null ?null :cmp.getShift().getOperand();
            replace(cmp, op1);
            if (op2 instanceof VirtualRegister)
                replace(cmp, ((VirtualRegister) op2));
            if (shift instanceof VirtualRegister)
                replace(cmp, ((VirtualRegister) shift));
        }
        else if (inst instanceof MCload) {
            var load = ((MCload) inst);
            var dst = load.getDst();
            var addr = load.getAddr();
            var offset = load.getOffset();
            replace(load, dst);
            replace(load, addr);
            if (offset instanceof VirtualRegister)
                replace(load, ((VirtualRegister) offset));
        }
        else if (inst instanceof MCstore) {
            var store = ((MCstore) inst);
            var src = store.getSrc();
            var addr = store.getAddr();
            var offset = store.getOffset();
            replace(store, src);
            replace(store, addr);
            if (offset instanceof VirtualRegister)
                replace(store, ((VirtualRegister) offset));
        }
        else if (inst instanceof MCMove) {
            var move = ((MCMove) inst);
            var dst = move.getDst();
            var src = move.getSrc();
            var shift = move.getShift()==null ?null :move.getShift().getOperand();
            replace(move, dst);
            if (src instanceof VirtualRegister)
                replace(move, ((VirtualRegister) src));
            if (shift instanceof VirtualRegister)
                replace(move, ((VirtualRegister) shift));
        }
        else if (inst instanceof MCFma) {
            var fma = ((MCFma) inst);
            Register accumulate = fma.getAccumulate();
            Register multiple_1 = fma.getMultiple_1();
            Register multiple_2 = fma.getMultiple_2();
            Register dst = fma.getDst();
            replace(fma, accumulate);
            replace(fma, multiple_1);
            replace(fma, multiple_2);
            replace(fma, dst);
        }
    }

    private void replace(MCInstruction inst, Register old) {
        inst.replaceRegister(old, RealRegister.get(color.get(old)));
    }
    //</editor-fold>
}
