package backend.armCode.MCInstructions;

import backend.armCode.MCInstruction;
import backend.operand.MCOperand;
import backend.operand.Register;

public class MCstore extends MCInstruction {

    private Register src;
    private MCOperand addr;
    private MCOperand offset;

    public String emit(){
        return "STR " + src.emit() + ", " + addr.emit();
    }

    public void setSrc(Register src) {this.src = src;}
    public void setAddr(MCOperand addr) {this.addr = addr;}

    public MCstore(Register src, MCOperand addr) {super(TYPE.MOV); this.src = src; this.addr = addr;}
    public MCstore(Register src, MCOperand addr, Shift shift, ConditionField cond) {super(TYPE.MOV, shift, cond); this.src = src; this.addr = addr;}
}
