package passes.ir;

import java.util.List;

public interface IBBRelationship<BasicBlock extends IBBRelationship<BasicBlock>> {
    void addPreviousBasicBlock(BasicBlock previousBlock);

    void setFollowingBasicBlocks(List<BasicBlock> followingBasicBlocks);

}
