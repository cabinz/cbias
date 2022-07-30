package passes.ir;

import ir.Use;
import ir.values.Instruction;
import ir.values.instructions.PhiInst;
import ir.values.instructions.TerminatorInst;

import java.util.ArrayList;
import java.util.Collection;
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
            var followingBBs = getFollowingBB(basicBlock.getRawBasicBlock());
            List<BasicBlock> followingBasicBlocks = new ArrayList<>();
            for (ir.values.BasicBlock followingBB : followingBBs) {
                followingBasicBlocks.add(basicBlockMap.get(followingBB));
            }
            followingBasicBlocks.forEach(followingBasicBlock -> followingBasicBlock.addPreviousBasicBlock(basicBlock));
            basicBlock.setFollowingBasicBlocks(followingBasicBlocks);
        });
    }

    public static Collection<ir.values.BasicBlock> getFollowingBB(ir.values.BasicBlock basicBlock){
        var lastInst = basicBlock.getLastInst();
        var ret = new ArrayList<ir.values.BasicBlock>();
        if(!(lastInst instanceof TerminatorInst.Br)) return ret;
        var br = (TerminatorInst.Br) lastInst;
        if(br.isCondJmp()){
            ret.add((ir.values.BasicBlock) br.getOperandAt(1));
            ret.add((ir.values.BasicBlock) br.getOperandAt(2));
        }else{
            ret.add((ir.values.BasicBlock) br.getOperandAt(0));
        }
        return ret;
    }

    public static void removeEntry(ir.values.BasicBlock basicBlock, ir.values.BasicBlock entry) {
        for (Instruction instruction : basicBlock) {
            if (instruction instanceof PhiInst) {
                var phiInst = (PhiInst) instruction;
                phiInst.removeMapping(entry);
            } else {
                break; // PHI must be in the front of a bb
            }
        }
    }

    public static void freeBlock(ir.values.BasicBlock basicBlock){
        for (Instruction instruction : basicBlock.getInstructions()) {
            for (Use use : instruction.getOperands()) {
                instruction.removeOperandAt(use.getOperandPos());
            }
        }
    }
}
