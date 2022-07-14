package backend.armCode.MCInstructions;

import backend.armCode.MCBasicBlock;
import backend.armCode.MCFunction;
import backend.armCode.MCInstruction;
import backend.operand.RealRegister;
import backend.operand.Register;

import java.util.HashSet;

public class MCbranch extends MCInstruction {

    private String target;
    private boolean withLink;

    private MCFunction targetFunc;
    private MCBasicBlock targetBB;

    public boolean isBranch() {return !withLink;}

    public String emit() {
        if (withLink)
            return "BL " + target;
        else
            return "B" + emitCond() + " " + target;
    }

    @Override
    public HashSet<Register> getUse() {
        var set = new HashSet<Register>();
        if (withLink) {
            set.add(RealRegister.get(0));
            set.add(RealRegister.get(1));
            set.add(RealRegister.get(2));
            set.add(RealRegister.get(3));
            /* add lr */
            set.add(RealRegister.get(14));
        }
        return set;
    }

    @Override
    public HashSet<Register> getDef() {
        return getUse();
    }

    public MCFunction getTargetFunc() {return targetFunc;}
    public void setTargetFunc(MCFunction targetFunc) {this.targetFunc = targetFunc;}

    public MCBasicBlock getTargetBB() {return targetBB;}
    public void setTargetBB(MCBasicBlock targetBB) {this.targetBB = targetBB;}

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

    public MCbranch(MCBasicBlock target, ConditionField cond) {
        super(TYPE.BRANCH, null, cond);
        this.target = target.getName();
        withLink = false;
        targetBB = target;
    }
}