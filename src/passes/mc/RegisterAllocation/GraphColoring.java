package passes.mc.RegisterAllocation;

import backend.ARMAssemble;
import backend.armCode.MCBasicBlock;
import backend.armCode.MCFunction;
import backend.armCode.MCInstruction;
import backend.armCode.MCInstructions.MCMove;
import backend.operand.RealRegister;
import backend.operand.Register;
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

    /**
     * a mapping from a node to the list of moves it is associated with
     */
    private HashMap<Register, HashSet<MCMove>> moveList;
    /**
     * Moves not yet ready for coalescing
     */
    private HashSet<MCMove> activeMoves;

    private HashSet<MCMove> coalescedMoves;
    private HashSet<MCMove> constrainedMoves;
    private HashSet<MCMove> frozenMove;
    //</editor-fold>

    //<editor-fold desc="Tools">
    private MCFunction curFunc;
    private HashMap<MCBasicBlock, LiveInfo> liveInfo;
    private final int INF = 0x3F3F3F3F;
    //</editor-fold>

    @Override
    public void runOnModule(ARMAssemble armAssemble) {
        /* main produce of the Graph Coloring */
        for (MCFunction func : armAssemble){
            curFunc = func;
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
                if (spillWorklist.isEmpty())
                    break;
                else
                    RewriteProgram();
            }
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
        degree = new HashMap<>(IntStream.range(0, 15)
                .mapToObj(RealRegister::get)
                .collect(Collectors.toMap(x -> x, x -> INF)));

        selectStack = new Stack<>();
        coalescedNodes = new HashSet<>();
        alias = new HashMap<>();
        spilledNodes = new HashSet<>();

        moveList = new HashMap<>();
        activeMoves = new HashSet<>();

        coalescedMoves = new HashSet<>();
        constrainedMoves = new HashSet<>();
        frozenMove = new HashSet<>();
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
                if (inst instanceof MCMove move && move.getShift() == null && move.getCond() == null) {
                    /* Delete the variable in live */
                    live.removeAll(uses);

                    /* Add this to moveList */
                    uses.forEach(use -> moveList.putIfAbsent(use, new HashSet<>()));
                    uses.forEach(use -> moveList.get(use).add(move));

                    defs.forEach(use -> moveList.putIfAbsent(use, new HashSet<>()));
                    defs.forEach(use -> moveList.get(use).add(move));

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

    private void Freeze() {
        var u = freezeWorklist.iterator().next();
        freezeWorklist.remove(u);
        simplifyWorklist.add(u);
        FreezeMoves(u);
    }

    private void SelectSpill() {
        // TODO: BETTER WAY
        var m = spillWorklist.iterator().next();
        spillWorklist.remove(m);
        simplifyWorklist.add(m);
        FreezeMoves(m);
    }

    private void AssignColors() {

    }

    private void RewriteProgram() {

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
             // TODO: copy(x, y) is x=y?
             var v = GetAlias(((Register) move.getSrc())) == GetAlias(u) ?GetAlias(move.getDst()) :GetAlias(((Register) move.getSrc()));

             // TODO: if？
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
    //</editor-fold>
}
