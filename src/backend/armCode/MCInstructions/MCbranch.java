package backend.armCode.MCInstructions;

import backend.armCode.MCInstruction;

public class MCbranch extends MCInstruction {

    private String target;

    private boolean withLink;


    public String emit() {
        if (withLink)
            return "BL " + target;
        else
            return "B" + emitCond() + " " + target;
    }

    public MCbranch(String target) {super(TYPE.BRANCH);this.target = target;withLink = false;}
    public MCbranch(String target, boolean withLink) {super(TYPE.BRANCH);this.target = target;this.withLink = withLink;}

}