package passes.mc.RegisterAllocation;

import backend.ARMAssemble;
import backend.armCode.MCFunction;
import backend.armCode.MCInstructions.MCMove;
import backend.operand.MCOperand;
import passes.mc.MCPass;

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

    @Override
    public void runOnModule(ARMAssemble armAssemble) {
        for (MCFunction func : armAssemble){
            while (true) {
                var liveInfoMap = LivenessAnalysis.run(func);
                Build();
                MakeWorklist();
                do {
                    if (!simplifyWorklist.isEmpty()) Simplfy();
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
}
