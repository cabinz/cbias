package passes.ir.gv_localize;

import passes.ir.IBBRelationship;

import java.util.ArrayList;
import java.util.List;

public class BasicBlock extends passes.ir.BasicBlock implements IBBRelationship<BasicBlock> {

    public BasicBlock(ir.values.BasicBlock basicBlock){
        super(basicBlock);
    }

    List<BasicBlock> previousBasicBlocks = new ArrayList<>();
    List<BasicBlock> followingBasicBlocks = new ArrayList<>();

    @Override
    public void addPreviousBasicBlock(BasicBlock previousBlock) {
        previousBasicBlocks.add(previousBlock);
    }

    @Override
    public void setFollowingBasicBlocks(List<BasicBlock> followingBasicBlocks) {
        this.followingBasicBlocks = followingBasicBlocks;
    }


}
