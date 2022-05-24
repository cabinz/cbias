package backend.armCode.MCInstructions;

import backend.armCode.MCInstruction;
import backend.operand.MCOperand;
import backend.operand.Register;

public class MCload extends MCInstruction {

    private Register dst;
    private MCOperand addr;

    public String emit(){
        return "LDR " + dst.emit() + ", " + addr.emit();
    }

    public void setDst(Register dst) {this.dst = dst;}
    public void setAddr(MCOperand addr) {this.addr = addr;}

    public MCload(Register dst, MCOperand addr) {super(TYPE.MOV); this.dst = dst; this.addr = addr;}
    public MCload(Register dst, MCOperand addr, Shift shift, ConditionField cond) {super(TYPE.MOV, shift, cond); this.dst = dst; this.addr = addr;}
}
