package passes.mc.mergeBlock;

import backend.ARMAssemble;
import backend.armCode.MCBasicBlock;
import backend.armCode.MCFunction;
import backend.armCode.MCInstructions.MCReturn;
import backend.armCode.MCInstructions.MCbranch;
import passes.mc.MCPass;

import java.util.HashSet;
import java.util.LinkedList;

public class MergeBlock implements MCPass {

    @Override
    public void runOnModule(ARMAssemble armAssemble) {
        for (MCFunction func : armAssemble) {
            var new_list = new LinkedList<MCBasicBlock>();
            mergeBlock(func.getEntryBlock(), new_list);
            func.setBasicBlockList(serialize(new_list));
        }
    }

    private void mergeBlock(MCBasicBlock dealing, LinkedList<MCBasicBlock> new_list) {
        var visited = new HashSet<MCBasicBlock>();
        mergeBlock(dealing, dealing, new_list, visited);
    }

    /**
     * Merge block and sort the basic block by DFS order
     * @param dealing the basic block to be analysed
     * @param insert the block which merged block will be inserted to
     * @param new_list the reordered list by DFS
     * @param visited the visited block
     */
    private void mergeBlock(MCBasicBlock dealing, MCBasicBlock insert, LinkedList<MCBasicBlock> new_list, HashSet<MCBasicBlock> visited) {
        if (visited.contains(dealing))
            return;
        visited.add(dealing);
        if (!new_list.contains(insert))
            new_list.add(insert);

        /* Meaning it's a return block */
        if (dealing.getTrueSuccessor() == null)
            return;

        /* DFS */
        if (dealing.getFalseSuccessor() == null) {
            var onlySucc = dealing.getTrueSuccessor();
            if (onlySucc.getPredecessors().size() == 1) {
                insert.removeLast();
                insert.appendAndRemove(onlySucc);
                insert.setTrueSuccessor(onlySucc.getTrueSuccessor());
                insert.setFalseSuccessor(onlySucc.getFalseSuccessor());
                mergeBlock(insert, insert, new_list, visited);
            }
            else
                mergeBlock(onlySucc, onlySucc, new_list, visited);
        }
        else {
            var trueSucc = dealing.getTrueSuccessor();
            var falseSucc = dealing.getFalseSuccessor();
            if (trueSucc.getPredecessors().size() == 1) {
                insert.removeSecondLast();
                insert.appendAndRemove(trueSucc);
                insert.setTrueSuccessor(trueSucc.getTrueSuccessor());
                mergeBlock(trueSucc, insert, new_list, visited);
                mergeBlock(falseSucc, falseSucc, new_list, visited);
            }
            else if (falseSucc.getPredecessors().size() == 1){
                insert.removeLast();
                insert.appendAndRemove(falseSucc);
                insert.setTrueSuccessor(trueSucc.getTrueSuccessor());
                mergeBlock(falseSucc, insert, new_list, visited);
                mergeBlock(trueSucc, trueSucc, new_list, visited);
            }
            else {
                mergeBlock(trueSucc, trueSucc, new_list, visited);
                mergeBlock(falseSucc, falseSucc, new_list, visited);
            }
        }
    }

    /**
     * Delete the last branch and order the successor behind the block if possible <br/>
     * Resort the BasicBlock list in the order of DFS, which may cause the block index discontinuous
     * @param list the list to be worked
     * @return the sorted list
     */
    private LinkedList<MCBasicBlock> serialize(LinkedList<MCBasicBlock> list) {
        var serializedList = new LinkedList<MCBasicBlock>();
        while (!list.isEmpty()) {
            /* Following the DFS order for the longest branch chain */
            MCBasicBlock dealing = list.getFirst();
            while (dealing != null) {
                serializedList.add(dealing);
                list.remove(dealing);

                if (dealing.getLastInst() instanceof MCReturn) {
                    break;
                }
                else {
                    var branch = ((MCbranch) dealing.getLastInst());
                    assert branch.isBranch();
                    if (branch.getCond() == null) {
                        var target = branch.getTargetBB();
                        if (serializedList.contains(target))
                            break;
                        else {
                            dealing.removeLast();
                            dealing = target;
                        }
                    }
                    else {
                        var trueBlock = ((MCbranch) dealing.getSecondLastInst()).getTargetBB();
                        var falseBlock = ((MCbranch) dealing.getLastInst()).getTargetBB();
                        if (!serializedList.contains(trueBlock)) {
                            dealing.removeSecondLast();
                            dealing = trueBlock;
                        }
                        else if (!serializedList.contains(falseBlock)){
                            dealing.removeLast();
                            dealing = falseBlock;
                        }
                        else
                            break;
                    }
                }
            }
        }
        return serializedList;
    }
}
