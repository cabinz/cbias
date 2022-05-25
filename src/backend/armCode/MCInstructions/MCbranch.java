package backend.armCode.MCInstructions;

import backend.armCode.MCBasicBlock;
import backend.armCode.MCInstruction;
import backend.operand.MCOperand;

public class MCbranch extends MCInstruction {

    private String target;

    private boolean withLink;


    public String emit() {
        if (withLink)
            return "BL " + target;
        else
            return "B " + target;
    }

    public MCbranch(String target) {super(TYPE.B);this.target = target;withLink = false;}
    public MCbranch(String target, boolean withLink) {super(TYPE.B);this.target = target;this.withLink = withLink;}

}