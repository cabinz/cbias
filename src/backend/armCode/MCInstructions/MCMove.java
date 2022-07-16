package backend.armCode.MCInstructions;

import backend.MCBuilder;
import backend.armCode.MCInstruction;
import backend.operand.Immediate;
import backend.operand.MCOperand;
import backend.operand.Register;

import java.util.HashSet;

/**
 * This class represent the move operation of ARM. <br/>
 * It's an aggregation too, containing MOV, MOVW, MOVT, MNV.
 */
public class MCMove extends MCInstruction {

    private Register dst;
    private MCOperand src;

    private boolean exceededLimit = false;

    public boolean isCopy() {return src.isVirtualReg() || src.isRealReg();}

    @Override
    public HashSet<Register> getUse() {
        var set = new HashSet<Register>();
        if (src.isVirtualReg() || src.isRealReg())
            set.add(((Register) src));
        if (shift != null && shift.getOperand().isVirtualReg())
            set.add(((Register) shift.getOperand()));
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
        if (src == old) src = tmp;
        if (shift != null && shift.getOperand() == old) shift.setRegister(tmp);
    }

    public String emit(){
        if (src.isGlobalVar())
            return "MOVW" + emitCond() + ' ' + dst.emit() + ", :lower16:" + src.emit() + "\n\tMOVT" + emitCond() + ' ' + dst.emit() + ", :upper16:" + src.emit();
        else if (exceededLimit) {
            int value = ((Immediate) src).getIntValue();
            if (MCBuilder.canEncodeImm(-value)) {
                return "MVN" + emitCond() + ' ' + dst.emit() + ", #" + -value;
            }
            else {
                int high16 = value >>> 16;
                int low16 = value & 0xFFFF;
                if (high16 == 0)
                    return "MOVW" + emitCond() + ' ' + dst.emit() + ", #" + low16;
                else
                    return "MOVW" + emitCond() + ' ' + dst.emit() + ", #" + low16 + "\n\tMOVT" + emitCond() + ' ' + dst.emit() + ", #" + high16;
            }
        }
        else
            return "MOV" + emitCond() + ' ' + dst.emit() + ", " + src.emit() + emitShift();
    }

    public Register getDst() {return dst;}
    public void setDst(Register dst) {this.dst = dst;}

    public MCOperand getSrc() {return src;}
    public void setSrc(MCOperand src) {this.src = src;}

    public MCMove(Register dst, MCOperand src) {super(TYPE.MOV); this.dst = dst; this.src = src;}
    public MCMove(Register dst, MCOperand src, boolean exceededLimit) {super(TYPE.MOV); this.dst = dst; this.src = src; this.exceededLimit=exceededLimit;}
    public MCMove(Register dst, MCOperand src, Shift shift, ConditionField cond) {super(TYPE.MOV, shift, cond); this.dst = dst; this.src = src;}
}
