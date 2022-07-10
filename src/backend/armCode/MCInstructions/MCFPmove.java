package backend.armCode.MCInstructions;

import backend.armCode.MCInstruction;
import backend.operand.ExtensionRegister;
import backend.operand.Immediate;
import backend.operand.MCOperand;
import backend.operand.Register;

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
public class MCFPmove extends MCInstruction {

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
    public String emit() {
        if (doubleMove) {
            // TODO: 不是相邻的扩展寄存器时报错
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
    public MCFPmove(ExtensionRegister dst1, Immediate src1) {super(TYPE.VMOV);this.src1 = src1;this.dst1 = dst1;doubleMove=false;}
    public MCFPmove(Register dst1, ExtensionRegister src1) {super(TYPE.VMOV);this.src1 = src1;this.dst1 = dst1;doubleMove=false;}
    public MCFPmove(ExtensionRegister dst1, Register src1) {super(TYPE.VMOV);this.src1 = src1;this.dst1 = dst1;doubleMove=false;}
    public MCFPmove(ExtensionRegister dst1, ExtensionRegister src1) {super(TYPE.VMOV);this.src1 = src1;this.dst1 = dst1;doubleMove=false;}

    /* Double move */
    public MCFPmove(ExtensionRegister dst1, ExtensionRegister dst2, Register src1, Register src2) {super(TYPE.VMOV);this.src1 = src1;this.dst1 = dst1;this.src2 = src2;this.dst2 = dst2;doubleMove = true;}
    public MCFPmove(Register dst1, Register dst2, ExtensionRegister src1, ExtensionRegister src2) {super(TYPE.VMOV);this.src1 = src1;this.dst1 = dst1;this.src2 = src2;this.dst2 = dst2;doubleMove = true;}
}
