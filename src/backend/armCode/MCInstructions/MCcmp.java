package backend.armCode.MCInstructions;

import backend.armCode.MCInstruction;
import backend.operand.MCOperand;
import backend.operand.Register;

public class MCcmp extends MCInstruction {

    private Register operand1;
    private MCOperand operand2;

    public Register getOperand1() {return operand1;}
    public void setOperand1(Register operand1) {this.operand1 = operand1;}

    public MCOperand getOperand2() {return operand2;}
    public void setOperand2(MCOperand operand2) {this.operand2 = operand2;}

    @Override
    public String emit() {
        return "CMP " + operand1.emit() + ", " + operand2.emit();
    }

    public MCcmp(Register operand1, MCOperand operand2) {super(TYPE.CMP);this.operand1 = operand1;this.operand2 = operand2;}
}
