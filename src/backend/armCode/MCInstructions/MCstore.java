package backend.armCode.MCInstructions;

import backend.armCode.MCInstruction;
import backend.operand.MCOperand;
import backend.operand.Register;

public class MCstore extends MCInstruction {

    private Register src;
    private MCOperand addr;
    private MCOperand offset;

    public String emit(){
        return "STR " + src.emit() + ", " + addr.emit() ;
    }

    public void setSrc(Register src) {this.src = src;}
    public void setAddr(MCOperand addr) {this.addr = addr;}
    public void setOffset(MCOperand offset) {this.offset = offset;}

    public MCstore(Register src, MCOperand addr) {super(TYPE.STORE); this.src = src; this.addr = addr;}
    public MCstore(Register src, MCOperand addr, MCOperand offset) {super(TYPE.STORE);this.src = src;this.addr = addr;this.offset = offset;}
    public MCstore(Register src, MCOperand addr, Shift shift, ConditionField cond) {super(TYPE.STORE, shift, cond); this.src = src; this.addr = addr;}
}
