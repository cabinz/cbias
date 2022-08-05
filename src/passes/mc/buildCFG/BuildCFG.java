package passes.mc.buildCFG;

import backend.ARMAssemble;
import backend.armCode.MCBasicBlock;
import backend.armCode.MCFunction;
import backend.armCode.MCInstruction;
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
                MCInstruction lastInst = block.getLastInst();
                /* End with branch */
                if (lastInst instanceof MCbranch && ((MCbranch) lastInst).isBranch()) {
                    var branch1 = ((MCbranch) lastInst);
                    var target1 = branch1.getTargetBB();
                    target1.addPredecessor(block);
                    if (branch1.getCond() == null) {
                        block.setTrueSuccessor(target1);
                    }
                    else {
                        block.setFalseSuccessor(target1);
                        var branch2 = ((MCbranch) block.getSecondLastInst());
                        block.setTrueSuccessor(branch2.getTargetBB());
                        branch2.getTargetBB().addPredecessor(block);
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
