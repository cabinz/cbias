package passes.ir.simplify;

import passes.ir.IBBRelationship;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class BasicBlock extends passes.ir.BasicBlock implements IBBRelationship<BasicBlock> {

    public Set<BasicBlock> prevBlocks = new HashSet<>();
    public Set<BasicBlock> followingBlocks = new HashSet<>();

    public BasicBlock(ir.values.BasicBlock rawBasicBlock) {
        super(rawBasicBlock);
    }

    @Override
    public void addPreviousBasicBlock(BasicBlock previousBlock) {
        prevBlocks.add(previousBlock);
    }

    @Override
    public void setFollowingBasicBlocks(List<BasicBlock> followingBasicBlocks) {
        followingBlocks.addAll(followingBasicBlocks);
    }

}
