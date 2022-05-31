package backend.armCode.MCInstructions;

import backend.armCode.MCInstruction;
import backend.operand.MCOperand;
import backend.operand.Register;

/**
 * The STR instruction of ARM. <br/>
 * Now just has the pre‐indexed addressing and write option.
 */
public class MCstore extends MCInstruction {

    private Register src;
    private MCOperand addr;
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

    public String emit(){
        return "STR " + src.emit() + ", [" + addr.emit() + (offset==null ?"" :", "+offset.emit()) + "]" + (write?"!":"");
    }

    //<editor-fold desc="Getter & Setter">
    public Register getSrc() {return src;}
    public MCOperand getAddr() {return addr;}
    public MCOperand getOffset() {return offset;}

    public void setSrc(Register src) {this.src = src;}
    public void setAddr(MCOperand addr) {this.addr = addr;}
    public void setOffset(MCOperand offset) {this.offset = offset;}
    //</editor-fold>

    //<editor-fold desc="Constructor">
    public MCstore(Register src, MCOperand addr) {super(TYPE.STORE); this.src = src; this.addr = addr;}
    public MCstore(Register src, MCOperand addr, MCOperand offset) {super(TYPE.STORE);this.src = src;this.addr = addr;this.offset = offset;this.write=false;}
    public MCstore(Register src, MCOperand addr, MCOperand offset, boolean write) {super(TYPE.STORE);this.src = src;this.addr = addr;this.offset = offset;this.write=write;}
    public MCstore(Register src, MCOperand addr, Shift shift, ConditionField cond) {super(TYPE.STORE, shift, cond); this.src = src; this.addr = addr;}
    //</editor-fold>
}
