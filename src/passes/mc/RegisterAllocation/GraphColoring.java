package passes.mc.RegisterAllocation;

import backend.ARMAssemble;
import backend.armCode.MCBasicBlock;
import backend.armCode.MCFunction;
import backend.armCode.MCInstruction;
import backend.armCode.MCInstructions.MCMove;
import backend.operand.MCOperand;
import backend.operand.Register;
import backend.operand.VirtualRegister;
import passes.mc.MCPass;

import java.util.HashMap;
import java.util.HashSet;


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

    //<editor-fold desc="Key set">
    /**
     * The set of low-degree non-move-related nodes
     */
    private HashSet<MCOperand> simplifyWorklist;
    /**
     * The set of move instructions that might be coalescing
     */
    private HashSet<MCMove> worklistMoves;
    /**
     * The set of low-degree move-related nodes
     */
    private HashSet<MCOperand> freezeWorklist;
    /**
     * The set of high-degree nodes
     */
    private HashSet<MCOperand> spillWorklist;
    //</editor-fold>

    /**
     * a mapping from a node to the list of moves it is associated with
     */
    private HashMap<Register, HashSet<MCMove>> moveList;

    //<editor-fold desc="Tools">
    private MCFunction curFunc;
    private HashMap<MCBasicBlock, LiveInfo> liveInfo;
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

    private void Initialize() {
        simplifyWorklist = new HashSet<>();
        worklistMoves = new HashSet<>();
        freezeWorklist = new HashSet<>();
        spillWorklist = new HashSet<>();

        moveList = new HashMap<>();
    }

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
                /* live = union(def, live) */
                live.addAll(defs);

                /* Build the RIG */
                /* Add an edge from def to each node in live */
                defs.forEach(d -> live.forEach(l -> addEdge(l, d)));

                /* live = union(uses, live\defs) */
                live.removeAll(defs);
                live.addAll(uses);
            }
        }
    }

    private void MakeWorklist() {

    }

    private void Simplify() {

    }

    private void Coalesce() {

    }

    private void Freeze() {

    }

    private void SelectSpill() {

    }

    private void AssignColors() {

    }

    private void RewriteProgram() {

    }
}
