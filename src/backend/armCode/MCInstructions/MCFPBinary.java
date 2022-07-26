package backend.armCode.MCInstructions;

import backend.armCode.MCInstruction;
import backend.operand.ExtensionRegister;
import backend.operand.Register;

import java.util.HashSet;

/**
 * This class is a aggregation of the BINARY data-processing <br/>
 * instruction in ARM, including the VADD, VSUB, VMUL, VDIV. <br/>
 * MCFP = Machine Code Float Point
 * @see <a href="https://developer.arm.com/documentation/ddi0406/latest/">
 *     ARM Architecture Reference Manual ARMv7 edition </a> <br/>
 *     A4.14 Page: A4-38
 */
public class MCFPBinary extends MCInstruction {

    private ExtensionRegister operand1;
    private ExtensionRegister operand2;
    private ExtensionRegister destination;

    @Override
    public HashSet<Register> getUse() {
        return new HashSet<>();
    }

    @Override
    public HashSet<Register> getDef() {
        return new HashSet<>();
    }

    @Override
    public void replaceRegister(Register old, Register tmp) {
    }

    public String emit() {
        return type.name() + emitCond() + ".F32 " + destination.emit()
                + ", " + operand1.emit() + ", " + operand2.emit();
    }

    public MCFPBinary(TYPE type, ExtensionRegister destination, ExtensionRegister operand1, ExtensionRegister operand2) {
        super(type);
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.destination = destination;
    }
}
