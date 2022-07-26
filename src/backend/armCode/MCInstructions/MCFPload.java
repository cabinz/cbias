package backend.armCode.MCInstructions;

import backend.armCode.MCInstruction;
import backend.operand.ExtensionRegister;
import backend.operand.Immediate;
import backend.operand.Register;

import java.util.HashSet;

/**
 * This class represent the VLDR instruction.<br/>
 * Format: VLDR Fd, [Rn{, #&lt;immed&gt;}]
 */
public class MCFPload extends MCInstruction {

    private ExtensionRegister dst;
    private Register addr;
    /**
     * The offset must be an integer number between 0 and 1020,<br/>
     * which can be divided by 4.
     */
    private Immediate offset;

    @Override
    public HashSet<Register> getUse() {
        var set = new HashSet<Register>();
        set.add(addr);
        return set;
    }

    @Override
    public HashSet<Register> getDef() {
        return null;
    }

    @Override
    public void replaceRegister(Register old, Register tmp) {
        if (addr == old) addr = tmp;
    }

    @Override
    public String emit() {
        return "VLDR" + emitCond() + ' ' + dst.emit() + ", [" + addr.emit() + (offset==null ?"" :", "+offset.emit()) + "]";
    }

    public ExtensionRegister getDst() {return dst;}
    public Register getAddr() {return addr;}
    public Immediate getOffset() {return offset;}

    public void setDst(ExtensionRegister dst) {this.dst = dst;}
    public void setAddr(Register addr) {this.addr = addr;}
    public void setOffset(Immediate offset) {this.offset = offset;}

    public MCFPload(ExtensionRegister dst, Register addr) {super(TYPE.VLDR);this.dst = dst;this.addr = addr;}
    public MCFPload(ExtensionRegister dst, Register addr, Immediate offset) {super(TYPE.VLDR);this.dst = dst;this.addr = addr;this.offset = offset;}
}