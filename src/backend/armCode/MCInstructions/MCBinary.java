package backend.armCode.MCInstructions;

import backend.armCode.MCBasicBlock;
import backend.armCode.MCInstruction;
import backend.operand.MCOperand;
import backend.operand.Register;

/**
 * This class represent the arithmetical instruction of ARM,
 * including ADD, SUB, MUL. The first operand must be a register. <br/>
 * This class is an aggregation. This is why it starts with a capital.
 */
public class MCBinary extends MCInstruction {

    private Register operand1;
    private MCOperand operand2;
    private Register destination;

    public String emit() {
        return type.name() + ' ' + destination.emit() + ", " + operand1.emit() + ", " + operand2.emit();
    }

    public MCBinary(TYPE type, Register destination, Register operand1, MCOperand operand2) {super(type);this.destination = destination;this.operand1 = operand1;this.operand2 = operand2;}
    public MCBinary(TYPE type, Register destination, Register operand1, MCOperand operand2, Shift shift, ConditionField cond) {super(type, shift, cond);this.operand1 = operand1;this.operand2 = operand2;this.destination = destination;}
}
