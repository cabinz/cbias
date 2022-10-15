package passes.ir.simplify;

import passes.ir.analysis.IRelationAnalysis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * BasicBlock with extra information.
 */
class BasicBlock extends passes.ir.BasicBlock implements IRelationAnalysis<BasicBlock> {

    public Set<BasicBlock> prevBlocks = new HashSet<>();
    public Set<BasicBlock> followingBlocks = new HashSet<>();

    public BasicBlock(ir.values.BasicBlock rawBasicBlock) {
        super(rawBasicBlock);
    }

    @Override
    public void addEntryBlock(BasicBlock entryBlock) {
        prevBlocks.add(entryBlock);
    }

    @Override
    public void setExitBlocks(List<BasicBlock> exitBlocks) {
        followingBlocks.addAll(exitBlocks);
    }

}
