package backend.armCode.MCInstructions;

import backend.armCode.MCBasicBlock;
import backend.armCode.MCInstruction;
import backend.operand.MCOperand;
import backend.operand.Register;

public class MCBinary extends MCInstruction {

    private MCOperand operand1;
    private MCOperand operand2;
    private Register destination;

    public String emit() {
        return type.name() + ' ' + destination.emit() + ", " + operand1.emit() + ", " + operand2.emit();
    }

    public MCBinary(TYPE type, MCBasicBlock BasicBlock, Register destination, MCOperand operand1, MCOperand operand2) {super(type, BasicBlock);this.destination = destination;this.operand1 = operand1;this.operand2 = operand2;}
    public MCBinary(TYPE type, MCBasicBlock belongingBB, Register destination, MCOperand operand1, MCOperand operand2, Shift shift, ConditionField cond) {super(type, belongingBB, shift, cond);this.operand1 = operand1;this.operand2 = operand2;this.destination = destination;}
}
