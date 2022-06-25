package backend.armCode.MCInstructions;

import backend.armCode.MCInstruction;
import backend.operand.MCOperand;
import backend.operand.Register;

public class MCmov extends MCInstruction {

    private Register dst;
    private MCOperand src;

    public String emit(){
        return "MOV " + dst.emit() + ", " + src.emit();
    }

    public MCmov(Register dst, MCOperand src) {super(TYPE.MOV); this.dst = dst; this.src = src;}
    public MCmov(Register dst, MCOperand src, ConditionField cond) {super(TYPE.MOV, null, cond); this.dst = dst; this.src = src;}
}
