package passes.ir;

import ir.values.instructions.TerminatorInst;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RelationAnalysis {

    /**
     * Analysis relationship of the given basic blocks.
     * @param basicBlockMap Mapping from raw BB to wrapped BB.
     * @param <BasicBlock> The type of wrapped BB.
     */
    public static <BasicBlock extends passes.ir.BasicBlock & IBBRelationship<BasicBlock>>
    void analysisBasicBlocks(Map<ir.values.BasicBlock, BasicBlock> basicBlockMap){
        basicBlockMap.values().forEach(basicBlock -> {
            var lastInstruction = basicBlock.getRawBasicBlock().getLastInst();
            if(lastInstruction.isBr()){
                var br = (TerminatorInst.Br) lastInstruction;
                List<BasicBlock> followingBasicBlocks = new ArrayList<>();
                if(br.isCondJmp()){
                    followingBasicBlocks.add(basicBlockMap.get((ir.values.BasicBlock) br.getOperandAt(1)));
                    followingBasicBlocks.add(basicBlockMap.get((ir.values.BasicBlock) br.getOperandAt(2)));
                }else{
                    followingBasicBlocks.add(basicBlockMap.get((ir.values.BasicBlock) br.getOperandAt(0)));
                }
                followingBasicBlocks.forEach(followingBasicBlock -> followingBasicBlock.addPreviousBasicBlock(basicBlock));
                basicBlock.setFollowingBasicBlocks(followingBasicBlocks);
            }
        });
    }

}
