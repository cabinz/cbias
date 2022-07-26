package backend.armCode.MCInstructions;

import backend.armCode.MCFPInstruction;
import backend.armCode.MCInstruction;
import backend.operand.*;

import java.util.HashSet;

/**
 * This class represent the register transfer instructions of VFP, <br/>
 * including VMOV & VMRS. The instruction is legal ONLY when transferring:<br/>
 * &#09; an immediate to extension <br/>
 * &#09; from core to extension <br/>
 * &#09; from extension to core <br/>
 * &#09; from extension to extension (except MRS)<br/>
 * @see <a href="https://developer.arm.com/documentation/ddi0406/latest/">
 *     ARM Architecture Reference Manual ARMv7 edition </a> A4.12 Page: A4-29
 */
public class MCFPmove extends MCFPInstruction {

    private MCOperand src1;
    private MCOperand dst1;
    private MCOperand src2;
    private MCOperand dst2;

    /**
     * Mark this instruction whether move two register. <br/>
     * Can ONLY be used when transfer between core registers and extension registers. <br/>
     * Format: VMOV Sn, Sm, Rd, Rm   @Sn = Rd, Sm = Rn, <b>m = n + 1</b> <br/>
     * @see <a href="https://developer.arm.com/documentation/ddi0406/latest/">
     *     ARM Architecture Reference Manual ARMv7 edition </a> A8.6.331 Page: A8-650
     */
    private boolean doubleMove;

    @Override
    public HashSet<Register> getUse() {
        var set = new HashSet<Register>();
        if (src1.isVirtualReg() || src1.isRealReg()) set.add((Register) src1);
        if (src2.isVirtualReg() || src2.isRealReg()) set.add((Register) src2);
        return set;
    }

    @Override
    public HashSet<Register> getDef() {
        var set = new HashSet<Register>();
        if (dst1.isVirtualReg() || dst1.isRealReg()) set.add((Register) dst1);
        if (dst2.isVirtualReg() || dst2.isRealReg()) set.add((Register) dst2);
        return set;
    }

    @Override
    public void replaceRegister(Register old, Register tmp) {
        if (src1 == old) src1 = tmp;
        if (src2 == old) src2 = tmp;
        if (dst1 == old) dst1 = tmp;
        if (dst2 == old) dst2 = tmp;
    }

    @Override
    public HashSet<ExtensionRegister> getExtUse() {
        var set = new HashSet<ExtensionRegister>();
        if (src1.isVirtualExtReg() || src1.isRealExtReg()) set.add((ExtensionRegister) src1);
        if (src2.isVirtualExtReg() || src2.isRealExtReg()) set.add((ExtensionRegister) src2);
        return set;
    }

    @Override
    public HashSet<ExtensionRegister> getExtDef() {
        var set = new HashSet<ExtensionRegister>();
        if (dst1.isVirtualExtReg() || dst1.isRealExtReg()) set.add((ExtensionRegister) dst1);
        if (dst2.isVirtualExtReg() || dst2.isRealExtReg()) set.add((ExtensionRegister) dst2);
        return set;
    }

    @Override
    public void replaceExtReg(ExtensionRegister old, ExtensionRegister brand_new) {
        if (src1 == old) src1 = brand_new;
        if (src2 == old) src2 = brand_new;
        if (dst1 == old) dst1 = brand_new;
        if (dst2 == old) dst2 = brand_new;
    }

    @Override
    public String emit() {
        if (doubleMove) {
            return "VMOV" + emitCond() + ' ' + dst1.emit() + ", " + dst2.emit() + ", "
                    + src1.emit() + ", " + src2.emit();
        }
        else {
            if (src1 == null)
                return "VMRS" + emitCond() + " APSR_nzcv, FPSCR";
            else
                return "VMOV" + emitCond() + ' ' + dst1.emit() + ", " + src1.emit();
        }
    }

    /* Single move */
    /**
     * This constructor is designed to new a VMRS instruction.
     */
    public MCFPmove() {super(TYPE.VMRS);doubleMove=false;}
    public MCFPmove(ExtensionRegister dst1, FPImmediate src1) {super(TYPE.VMOV);this.src1 = src1;this.dst1 = dst1;doubleMove=false;}
    public MCFPmove(Register dst1, ExtensionRegister src1) {super(TYPE.VMOV);this.src1 = src1;this.dst1 = dst1;doubleMove=false;}
    public MCFPmove(ExtensionRegister dst1, Register src1) {super(TYPE.VMOV);this.src1 = src1;this.dst1 = dst1;doubleMove=false;}
    public MCFPmove(ExtensionRegister dst1, ExtensionRegister src1) {super(TYPE.VMOV);this.src1 = src1;this.dst1 = dst1;doubleMove=false;}
    public MCFPmove(MCOperand dst1, MCOperand src1) {super(TYPE.VMOV);this.src1 = src1;this.dst1 = dst1;doubleMove=false;}

    /* Double move */
    public MCFPmove(ExtensionRegister dst1, ExtensionRegister dst2, Register src1, Register src2) {super(TYPE.VMOV);this.src1 = src1;this.dst1 = dst1;this.src2 = src2;this.dst2 = dst2;doubleMove = true;}
    public MCFPmove(Register dst1, Register dst2, ExtensionRegister src1, ExtensionRegister src2) {super(TYPE.VMOV);this.src1 = src1;this.dst1 = dst1;this.src2 = src2;this.dst2 = dst2;doubleMove = true;}
}
