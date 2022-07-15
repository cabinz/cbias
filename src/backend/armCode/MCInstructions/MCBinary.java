package backend.armCode.MCInstructions;

import backend.armCode.MCInstruction;
import backend.operand.MCOperand;
import backend.operand.Register;

import java.util.HashSet;

/**
 * This class represent the arithmetical instruction of ARM,
 * including ADD, SUB, RSB, MUL, SDIV. The first operand must be a register. <br/>
 * This class is an aggregation. This is why it starts with a capital.
 */
public class MCBinary extends MCInstruction {

    private Register operand1;
    private MCOperand operand2;
    private Register destination;

    @Override
    public HashSet<Register> getUse() {
        var set = new HashSet<Register>();
        set.add(operand1);
        if (operand2.isVirtualReg() || operand2.isVirtualReg())
            set.add(((Register) operand2));
        return set;
    }

    @Override
    public HashSet<Register> getDef() {
        var set = new HashSet<Register>();
        set.add(destination);
        return set;
    }

    @Override
    public void replaceRegister(Register old, Register tmp) {
        if (operand1 == old) operand1 = tmp;
        if (operand2 == old) operand2 = tmp;
    }

    public String emit() {
        return type.name() + emitCond() + ' ' + destination.emit()
                + ", " + operand1.emit() + ", " + operand2.emit();
    }

    public MCBinary(TYPE type, Register destination, Register operand1, MCOperand operand2) {super(type);this.destination = destination;this.operand1 = operand1;this.operand2 = operand2;}
    public MCBinary(TYPE type, Register destination, Register operand1, MCOperand operand2, Shift shift, ConditionField cond) {super(type, shift, cond);this.operand1 = operand1;this.operand2 = operand2;this.destination = destination;}
}
