package backend.armCode.MCInstructions;

import backend.armCode.MCInstruction;
import backend.operand.ExtensionRegister;

/**
 * This class represents the VCVT in ARM. <br/>
 * The competition has only two type: signed int & float. <br/>
 * So this class only support the conversion between this two. <br/>
 * Format: <br/>
 * &#09; float2int: VCVT.S32.F32 Sd, Sm <br/>
 * &#09; int2float: VCVT.F32.S32 Sd, Sm
 * @see <a href="https://developer.arm.com/documentation/ddi0406/latest/">
 *     ARM Architecture Reference Manual ARMv7 edition </a><br/>
 *     A8.6.295  Page: A8-578
 */
public class MCFPconvert extends MCInstruction {

    private ExtensionRegister dst;
    private ExtensionRegister src;

    private final boolean f2i;

    @Override
    public String emit() {
        return "VCVT" + emitCond() + (f2i ?".S32.F32 " :".F32.S32 ") + dst.emit() + ", " + src.emit();
    }

    public ExtensionRegister getDst() {return dst;}
    public void setDst(ExtensionRegister dst) {this.dst = dst;}

    public ExtensionRegister getSrc() {return src;}
    public void setSrc(ExtensionRegister src) {this.src = src;}

    public MCFPconvert(ExtensionRegister dst, ExtensionRegister src, boolean f2i) {super(TYPE.VCVT);this.dst = dst;this.src = src;this.f2i = f2i;}
}