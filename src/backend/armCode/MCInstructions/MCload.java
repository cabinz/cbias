package backend.armCode.MCInstructions;

import backend.armCode.MCInstruction;
import backend.operand.Immediate;
import backend.operand.MCOperand;
import backend.operand.Register;

public class MCload extends MCInstruction {

    private Register dst;
    private MCOperand addr;
    private MCOperand offset;
    private boolean write;

    public String emit(){
        return "LDR " + addr.emit() + ", [" + addr.emit() + (offset==null ?"" :", "+offset.emit()) + "]" + (write?"!":"");
    }

    //<editor-fold desc="Getter & Setter">
    public Register getDst() {return dst;}
    public MCOperand getAddr() {return addr;}
    public MCOperand getOffset() {return offset;}

    public void setDst(Register dst) {this.dst = dst;}
    public void setAddr(MCOperand addr) {this.addr = addr;}
    public void setOffset(MCOperand offset) {this.offset = offset;}
    //</editor-fold>

    //<editor-fold desc="Constructor">
    public MCload(Register dst, MCOperand addr) {super(TYPE.MOV); this.dst = dst; this.addr = addr;}
    public MCload(Register dst, MCOperand addr, MCOperand offset) {super(TYPE.MOV); this.dst = dst; this.addr = addr; this.offset=offset; this.write=false;}
    public MCload(Register dst, MCOperand addr, MCOperand offset, boolean write) {super(TYPE.MOV); this.dst = dst; this.addr = addr; this.offset=offset; this.write=write;}
    //</editor-fold>
}
