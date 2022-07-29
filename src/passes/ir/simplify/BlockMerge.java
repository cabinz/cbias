package passes.ir.simplify;

import ir.Module;
import ir.values.Function;
import ir.values.Instruction;
import passes.ir.IRPass;
import passes.ir.RelationAnalysis;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class BlockMerge implements IRPass {
    @Override
    public void runOnModule(Module module) {
        optimize(module);
    }

    private static void optimize(Module module){
        // Build CFG
        Map<ir.values.BasicBlock, BasicBlock> basicBlockMap = new HashMap<>();
        for (Function function : module.functions) {
            for (ir.values.BasicBlock basicBlock : function) {
                basicBlockMap.put(basicBlock, new BasicBlock(basicBlock));
            }
        }
        RelationAnalysis.analysisBasicBlocks(basicBlockMap);

        Queue<BasicBlock> queue = new ArrayDeque<>();
        // Initial scan
        for (BasicBlock basicBlock : basicBlockMap.values()) {
            if(canCombine(basicBlock)){
                queue.add(basicBlock);
            }
        }

        // Cascade optimize
        while (!queue.isEmpty()){
            BasicBlock basicBlock = queue.remove();
            var prevBB = (BasicBlock) basicBlock.prevBlocks.toArray()[0];
            combine(basicBlockMap, prevBB, basicBlock);
        }
    }

    /**
     * Checks weather the block can be combined with the previous block.
     *
     * @param basicBlock The block to be checked.
     * @return True if it can be combined with the previous block.
     */
    private static boolean canCombine(BasicBlock basicBlock){
        if(basicBlock.prevBlocks.size()!=1) return false;
        var prevBB = (BasicBlock) basicBlock.prevBlocks.toArray()[0];
        return prevBB.followingBlocks.size() == 1;
    }

    private static void combine(Map<ir.values.BasicBlock, BasicBlock> basicBlockMap, BasicBlock prevBB, BasicBlock followingBB){
        prevBB.getRawBasicBlock().getLastInst().removeSelf();
        for (Instruction instruction : followingBB.getRawBasicBlock().getInstructions()) {
            instruction.removeSelf();
            prevBB.getRawBasicBlock().insertAtEnd(instruction);
        }
        prevBB.followingBlocks = followingBB.followingBlocks;
        basicBlockMap.remove(followingBB.getRawBasicBlock());
    }

}
