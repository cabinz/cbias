package passes.ir.dce;

import ir.Module;
import ir.values.BasicBlock;
import ir.values.Function;
import passes.ir.IRPass;
import passes.ir.analysis.RelationUtil;

import java.util.*;

/**
 * <p>Remove all unreachable codes.</p>
 *
 * <p>
 * The program will do a search to collect reachable blocks.
 * Then all blocks unreachable are removed.
 * </p>
 */
public class UnreachableCodeElim implements IRPass {

    @Override
    public void runOnModule(Module module) {
        module.functions.forEach(UnreachableCodeElim::optimize);
    }

    private static void optimize(Function function) {
//        removeNoEntryBlocks(function);
        removeFreeBlocks(function);
    }

    /**
     * Remove unreachable loops.
     */
    private static void removeFreeBlocks(Function function) {
        Set<BasicBlock> reachedBlocks = new HashSet<>();
        Queue<BasicBlock> judgeQueue = new ArrayDeque<>();
        judgeQueue.add(function.getEntryBB());
        reachedBlocks.add(function.getEntryBB());
        while (!judgeQueue.isEmpty()) {
            var curBlock = judgeQueue.remove();
            for (BasicBlock followingBlock : RelationUtil.getFollowingBB(curBlock)) {
                if (!reachedBlocks.contains(followingBlock)) {
                    judgeQueue.add(followingBlock);
                    reachedBlocks.add(followingBlock);
                }
            }
        }
        Set<BasicBlock> removeSet = new HashSet<>();
        for (BasicBlock basicBlock : function) {
            if (!reachedBlocks.contains(basicBlock)) {
                removeSet.add(basicBlock);
            }
        }
        for (BasicBlock basicBlock : removeSet) {
            var followedBlocks = RelationUtil.getFollowingBB(basicBlock);
            RelationUtil.freeBlock(basicBlock);
            for (BasicBlock followedBlock : followedBlocks) {
                if (!removeSet.contains(followedBlock)) {
                    RelationUtil.removeEntry(followedBlock, basicBlock);
                }
            }
        }
        for (BasicBlock basicBlock : removeSet) {
            basicBlock.markWasted();
        }
    }

}
