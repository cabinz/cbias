package passes.ir.analysis;

import ir.values.BasicBlock;

import java.util.List;
import java.util.Set;

/**
 * BasicBlock interface for CFG analysis.
 * @param <BasicBlock> Type where information are stored in.
 */
public interface IRelationAnalysis<BasicBlock extends IRelationAnalysis<BasicBlock>> {
    ir.values.BasicBlock getRawBasicBlock();
    void addEntryBlock(BasicBlock entryBlock);

    void setExitBlocks(List<BasicBlock> exitBlocks);

    default Set<BasicBlock> getEntryBlocks(){
        throw new UnsupportedOperationException();
    }

    default Set<BasicBlock> getExitBlocks(){
        throw new UnsupportedOperationException();
    }

}
