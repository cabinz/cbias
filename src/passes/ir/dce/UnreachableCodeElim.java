package passes.ir.dce;

import ir.Module;
import ir.values.BasicBlock;
import ir.values.Function;
import passes.ir.IRPass;
import passes.ir.RelationAnalysis;

import java.util.*;

public class UnreachableCodeElim implements IRPass {

    @Override
    public void runOnModule(Module module) {
        module.functions.forEach(UnreachableCodeElim::optimize);
    }

    private static void optimize(Function function){
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
        while (!judgeQueue.isEmpty()){
            var curBlock = judgeQueue.remove();
            for (BasicBlock followingBlock : RelationAnalysis.getFollowingBB(curBlock)) {
                if(!reachedBlocks.contains(followingBlock)){
                    judgeQueue.add(followingBlock);
                    reachedBlocks.add(followingBlock);
                }
            }
        }
        Set<BasicBlock> removeSet = new HashSet<>();
        for (BasicBlock basicBlock : function) {
            if(!reachedBlocks.contains(basicBlock)){
                removeSet.add(basicBlock);
            }
        }
        for (BasicBlock basicBlock : removeSet) {
            var followedBlocks = RelationAnalysis.getFollowingBB(basicBlock);
            RelationAnalysis.freeBlock(basicBlock);
            for (BasicBlock followedBlock : followedBlocks) {
                RelationAnalysis.removeEntry(followedBlock,basicBlock);
            }
        }
        for (BasicBlock basicBlock : removeSet) {
            basicBlock.markWasted();
        }
    }

    /**
     * Remove blocks with no entry.
     * <p> - No longer used since the other optimize contains this.
     * <p> - But why don't I delete this?
     * <p> - I don't know :(
     */
    private static void removeNoEntryBlocks(Function function) {
        // Maybe we should use RelationAnalysis here.
        Map<BasicBlock, Set<BasicBlock>> entryMap = new HashMap<>();
        function.forEach(basicBlock -> entryMap.put(basicBlock, new HashSet<>()));
        function.forEach(basicBlock -> {
            var followingBBs = RelationAnalysis.getFollowingBB(basicBlock);
            followingBBs.forEach(followingBB-> entryMap.get(followingBB).add(basicBlock));
        });
        Queue<BasicBlock> removeQueue = new ArrayDeque<>();
        entryMap.forEach((basicBlock, entrySet) -> {
            if(basicBlock!= function.getEntryBB()&&entrySet.isEmpty()){
                removeQueue.add(basicBlock);
            }
        });
        while (!removeQueue.isEmpty()){
            var basicBlock = removeQueue.remove();
            var followingBBs = RelationAnalysis.getFollowingBB(basicBlock);
            entryMap.remove(basicBlock);
            RelationAnalysis.freeBlock(basicBlock); // Free operands
            for (BasicBlock followingBB : followingBBs) {
                entryMap.get(followingBB).remove(basicBlock);
                RelationAnalysis.removeEntry(followingBB, basicBlock);
                if(entryMap.get(followingBB).isEmpty()){
                    removeQueue.add(followingBB);
                }
            }
            basicBlock.markWasted(); // Mark wasted
        }
    }

}
