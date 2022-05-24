package backend.armCode.MCInstructions;

import backend.armCode.MCBasicBlock;
import backend.armCode.MCInstruction;
import backend.operand.MCOperand;

public class MCbranch extends MCInstruction {

    private MCBasicBlock target;

    private boolean withLink;


    public String emit() {
        if (withLink)
            return "BL " + target.getName();
        else
            return "B " + target.getName();
    }

    public MCbranch(MCBasicBlock target) {super(TYPE.B);this.target = target;withLink = false;}
    public MCbranch(MCBasicBlock target, boolean withLink) {super(TYPE.B);this.target = target;this.withLink = withLink;}

}