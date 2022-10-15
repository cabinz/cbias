package passes.ir.simplify;

import ir.Module;
import ir.values.Function;
import ir.values.Instruction;
import passes.PassManager;
import passes.ir.IRPass;
import passes.ir.analysis.RelationAnalysis;
import passes.ir.analysis.RelationUtil;
import passes.ir.constant_derivation.ConstantDerivation;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * Merge blocks to reduce jmp instructions.
 */
public class BlockMerge implements IRPass {
    @Override
    public void runOnModule(Module module) {
        // Call ConstantDerivation to clear single phi
        PassManager.getInstance().run(ConstantDerivation.class, module);
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

        /*
        // Single br block elimination
        for (BasicBlock basicBlock : basicBlockMap.values()) {
            if(basicBlock.getRawBasicBlock().size()==1&&basicBlock.followingBlocks.size()==1){
                queue.add(basicBlock);
            }
        }
        while (!queue.isEmpty()){
            BasicBlock basicBlock = queue.remove();
            var followingBlock = (BasicBlock) basicBlock.followingBlocks.toArray()[0];
            for (Use use : basicBlock.getRawBasicBlock().getUses()) {
                var user = use.getUser();
                if(user instanceof TerminatorInst.Br){
                    use.setUsee(followingBlock.getRawBasicBlock());
                }
            }
        }
        */
    }

    /**
     * Checks whether the block can be combined with the previous block.
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
        // Move instructions
        prevBB.getRawBasicBlock().getLastInst().markWasted();
        for (Instruction instruction : followingBB.getRawBasicBlock().getInstructions()) {
            instruction.removeSelf();
            prevBB.getRawBasicBlock().insertAtEnd(instruction);
        }
        // Maintain CFG
        prevBB.followingBlocks = followingBB.followingBlocks;
        for (BasicBlock f2block : prevBB.followingBlocks) {
            f2block.prevBlocks.remove(followingBB);
            f2block.prevBlocks.add(prevBB);
            RelationUtil.replaceEntry(f2block.getRawBasicBlock(), followingBB.getRawBasicBlock(), prevBB.getRawBasicBlock());
        }
        basicBlockMap.remove(followingBB.getRawBasicBlock());
        // Remove from function
        followingBB.getRawBasicBlock().markWasted(); //Mark wasted
    }

}
