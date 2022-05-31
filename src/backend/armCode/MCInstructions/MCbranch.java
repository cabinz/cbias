package backend.armCode.MCInstructions;

import backend.armCode.MCBasicBlock;
import backend.armCode.MCFunction;
import backend.armCode.MCInstruction;

public class MCbranch extends MCInstruction {

    private String target;
    private boolean withLink;

    private MCFunction targetFunc;
    private MCBasicBlock targetBB;


    public String emit() {
        if (withLink)
            return "BL " + target;
        else
            return "B" + emitCond() + " " + target;
    }

    public MCbranch(MCFunction target) {
        super(TYPE.BRANCH);
        this.target = target.getName();
        withLink = true;
        targetFunc = target;
    }

    public MCbranch(MCBasicBlock target) {
        super(TYPE.BRANCH);
        this.target = target.getName();
        withLink = false;
        targetBB = target;
    }
}