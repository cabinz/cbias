package backend.armCode;

import backend.operand.MCOperand;

public class MCmov extends MCInstruction{

    private MCOperand L;
    private MCOperand R;

    public String emit(){
        return "MOV " + L.emit() + ", " + R.emit() + '\n';
    }

    public MCmov(MCBasicBlock BasicBlock, MCOperand L, MCOperand R) {super(TYPE.MOV, BasicBlock); this.L=L; this.R=R;}
    public MCmov(MCBasicBlock belongingBB, MCOperand L, MCOperand R, Shift shift, ConditionField cond) {super(TYPE.MOV, belongingBB, shift, cond); this.L=L; this.R=R;}
}
