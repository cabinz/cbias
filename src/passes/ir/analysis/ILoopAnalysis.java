package passes.ir.analysis;

/**
 * BasicBlock interface for loop analysis.
 * @param <BasicBlock> Type where information are stored in.
 */
public interface ILoopAnalysis<BasicBlock extends ILoopAnalysis<BasicBlock>> extends IDomAnalysis<BasicBlock> {

    void setLoop(LoopAnalysis<BasicBlock>.Loop loop);

    LoopAnalysis<BasicBlock>.Loop getLoop();

}
