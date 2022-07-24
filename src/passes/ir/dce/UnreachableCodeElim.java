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
        Map<BasicBlock, Set<BasicBlock>> entryMap = new HashMap<>();
        function.forEach(basicBlock -> entryMap.put(basicBlock, new HashSet<>()));
        function.forEach(basicBlock -> {
            var followingBBs = RelationAnalysis.getFollowingBB(basicBlock);
            followingBBs.forEach(followingBB-> entryMap.get(followingBB).add(basicBlock));
        });
        Queue<BasicBlock> removeQueue = new ArrayDeque<>();
        entryMap.forEach((basicBlock, entrySet) -> {
            if(basicBlock!=function.getEntryBB()&&entrySet.isEmpty()){
                removeQueue.add(basicBlock);
            }
        });
        while (!removeQueue.isEmpty()){
            var basicBlock = removeQueue.remove();
            var followingBBs = RelationAnalysis.getFollowingBB(basicBlock);
            entryMap.remove(basicBlock);
            basicBlock.removeSelf();
            for (BasicBlock followingBB : followingBBs) {
                entryMap.get(followingBB).remove(basicBlock);
                if(entryMap.get(followingBB).isEmpty()){
                    removeQueue.add(followingBB);
                }
            }
        }
    }

}
