package passes.mc.mergeBlock;

import backend.ARMAssemble;
import backend.armCode.MCBasicBlock;
import backend.armCode.MCFunction;
import passes.mc.MCPass;

import java.util.HashSet;

public class MergeBlock implements MCPass {

    @Override
    public void runOnModule(ARMAssemble armAssemble) {
        for (MCFunction func : armAssemble) {
            var visited = new HashSet<MCBasicBlock>();
            serialize(func.getEntryBlock(), func.getEntryBlock(), visited);
        }
    }

    public void serialize(MCBasicBlock dealing, MCBasicBlock insert, HashSet<MCBasicBlock> visited) {
        if (visited.contains(dealing))
            return;
        visited.add(dealing);

        /* Meaning it's a return block */
        if (dealing.getTrueSuccessor() == null)
            return;

        /* DFS */
        if (dealing.getFalseSuccessor() == null) {
            var onlySucc = dealing.getTrueSuccessor();
            if (onlySucc.getPredecessors().size() == 1) {
                insert.getInstructionList().removeLast();
                insert.appendAndRemove(onlySucc);
                insert.setTrueSuccessor(onlySucc.getTrueSuccessor());
                insert.setFalseSuccessor(onlySucc.getFalseSuccessor());
                serialize(insert, insert, visited);
            }
            else
                serialize(onlySucc, onlySucc, visited);
        }
        else {
            var trueSucc = dealing.getTrueSuccessor();
            var falseSucc = dealing.getFalseSuccessor();
            if (trueSucc.getPredecessors().size() == 1) {
                var list = insert.getInstructionList();
                list.remove(list.size()-2);
                insert.appendAndRemove(trueSucc);
                insert.setTrueSuccessor(trueSucc.getTrueSuccessor());
                serialize(trueSucc, insert, visited);
                serialize(falseSucc, falseSucc, visited);
            }
            else if (falseSucc.getPredecessors().size() == 1){
                insert.getInstructionList().removeLast();
                insert.appendAndRemove(falseSucc);
                insert.setTrueSuccessor(trueSucc.getTrueSuccessor());
                serialize(falseSucc, insert, visited);
                serialize(trueSucc, trueSucc, visited);
            }
            else {
                serialize(trueSucc, trueSucc, visited);
                serialize(falseSucc, falseSucc, visited);
            }
        }
    }
}
