package backend.armCode.MCInstructions;

import backend.armCode.MCInstruction;
import backend.operand.ExtensionRegister;
import backend.operand.Register;

import java.util.HashSet;

public class MCFPneg extends MCInstruction {

    private ExtensionRegister destination;
    private ExtensionRegister operand;

    @Override
    public HashSet<Register> getDef() {
        return new HashSet<>();
    }

    @Override
    public HashSet<Register> getUse() {
        return new HashSet<>();
    }

    @Override
    public void replaceRegister(Register old, Register tmp) {

    }

    @Override
    public String emit() {
        return "VNEG" + emitCond() + ".F32 " + destination.emit()
                + ", " + operand.emit();
    }

    public MCFPneg(ExtensionRegister destination, ExtensionRegister operand) {
        super(TYPE.VNEG);
        this.destination = destination;
        this.operand = operand;
    }
}
