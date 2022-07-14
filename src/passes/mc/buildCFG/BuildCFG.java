package passes.mc.buildCFG;

import backend.ARMAssemble;
import backend.armCode.MCBasicBlock;
import backend.armCode.MCFunction;
import backend.armCode.MCInstructions.MCReturn;
import backend.armCode.MCInstructions.MCbranch;
import passes.mc.MCPass;

public class BuildCFG implements MCPass {

    @Override
    public void runOnModule(ARMAssemble armAssemble) {
        for (MCFunction func : armAssemble) {
            for (int i=0; i<func.getBasicBlockList().size(); i++) {
                MCBasicBlock block = func.getBasicBlockList().get(i);
                MCBasicBlock next = i==func.getBasicBlockList().size()-1 ?null :func.getBasicBlockList().get(i+1);
                /* End with branch */
                if (block.getLastInst() instanceof MCbranch branch && branch.isBranch()) {
                    MCBasicBlock target = branch.getTargetBB();
                    block.setTrueSuccessor(target);
                    target.addPredecessor(block);
                    if (branch.getCond() != null && next != null) {
                        block.setFalseSuccessor(next);
                        next.addPredecessor(block);
                    }
                }
                /* End with return */
                else if (block.getLastInst() instanceof MCReturn){
                    continue;
                }
                /* else */
                else {
                    if (next != null) {
                        block.setTrueSuccessor(next);
                        next.addPredecessor(block);
                    }
                }
            }
        }
    }
}
