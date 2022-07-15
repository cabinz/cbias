package backend.armCode.MCInstructions;

import backend.armCode.MCInstruction;
import backend.operand.MCOperand;
import backend.operand.Register;

import java.util.HashSet;

public class MCload extends MCInstruction {

    private Register dst;
    private Register addr;
    /**
     * Addressing offset. <br/>
     * In ARM, this can be <br/>
     * &nbsp; - 12 bits immediate <br/>
     * &nbsp; - a register
     */
    private MCOperand offset;
    /**
     * Whether write to the sp
     */
    private boolean write;

    @Override
    public HashSet<Register> getUse() {
        var set = new HashSet<Register>();
        set.add(addr);
        if (offset != null && offset.isVirtualReg())
            set.add(((Register) offset));
        return set;
    }

    @Override
    public HashSet<Register> getDef() {
        var set = new HashSet<Register>();
        set.add(dst);
        return set;
    }

    @Override
    public void replaceRegister(Register old, Register tmp) {
        if (dst == old) dst = tmp;
        if (addr == old) addr = tmp;
        if (offset == old) offset = tmp;
    }

    public String emit(){
        return "LDR " + dst.emit() + ", [" + addr.emit() + (offset==null ?"" :", "+offset.emit()) + "]" + (write?"!":"");
    }

    //<editor-fold desc="Getter & Setter">
    public Register getDst() {return dst;}
    public Register getAddr() {return addr;}
    public MCOperand getOffset() {return offset;}

    public void setDst(Register dst) {this.dst = dst;}
    public void setAddr(Register addr) {this.addr = addr;}
    public void setOffset(MCOperand offset) {this.offset = offset;}
    //</editor-fold>

    //<editor-fold desc="Constructor">
    public MCload(Register dst, Register addr) {super(TYPE.MOV); this.dst = dst; this.addr = addr;}
    public MCload(Register dst, Register addr, MCOperand offset) {super(TYPE.MOV); this.dst = dst; this.addr = addr; this.offset=offset; this.write=false;}
    public MCload(Register dst, Register addr, MCOperand offset, boolean write) {super(TYPE.MOV); this.dst = dst; this.addr = addr; this.offset=offset; this.write=write;}
    //</editor-fold>
}
