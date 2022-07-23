package backend.armCode.MCInstructions;

import backend.armCode.MCInstruction;
import backend.operand.ExtensionRegister;
import backend.operand.Immediate;
import backend.operand.Register;

import java.util.HashSet;

/**
 * This class represents the VSTR instruction.
 */
public class MCFPstore extends MCInstruction {

    private ExtensionRegister src;
    private Register addr;
    /**
     * The offset must be an integer number between 0 and 1020,<br/>
     * which can be divided by 4.
     */
    private Immediate offset;

    @Override
    public HashSet<Register> getUse() {
        return null;
    }

    @Override
    public HashSet<Register> getDef() {
        return null;
    }

    @Override
    public void replaceRegister(Register old, Register tmp) {

    }

    @Override
    public String emit() {
        return "VSTR" + emitCond() + ' ' + src.emit() + ", [" + addr.emit() + (offset==null ?"" :", "+offset.emit()) + "]";
    }

    public ExtensionRegister getSrc() {return src;}
    public Register getAddr() {return addr;}
    public Immediate getOffset() {return offset;}

    public void setSrc(ExtensionRegister src) {this.src = src;}
    public void setAddr(Register addr) {this.addr = addr;}
    public void setOffset(Immediate offset) {this.offset = offset;}

    public MCFPstore(ExtensionRegister src, Register addr) {super(TYPE.VSTR);this.src = src;this.addr = addr;}
    public MCFPstore(ExtensionRegister src, Register addr, Immediate offset) {super(TYPE.VSTR);this.src = src;this.addr = addr;this.offset = offset;}
}
