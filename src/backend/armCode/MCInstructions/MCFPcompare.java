package backend.armCode.MCInstructions;

import backend.armCode.MCInstruction;
import backend.operand.ExtensionRegister;
import backend.operand.Register;

import java.util.HashSet;

/**
 * This class represent the VCMP. <br/>
 * One parameter constructor will compare to zero as default.
 */
public class MCFPcompare extends MCInstruction {

    private ExtensionRegister operand1;
    private ExtensionRegister operand2;

    @Override
    public HashSet<Register> getUse() {
        return null;
    }

    @Override
    public HashSet<Register> getDef() {
        return null;
    }

    @Override
    public void replaceRegister(Register old, Register tmp) {

    }

    @Override
    public String emit() {
        if (operand2 == null) {
            return "VCMP" + emitCond() + ' ' + operand1.emit() + ", #0.0";
        }
        else {
            return "VCMP" + emitCond() + ' ' + operand1.emit() + ", " + operand2.emit();
        }
    }

    public ExtensionRegister getOperand1() {return operand1;}
    public void setOperand1(ExtensionRegister operand1) {this.operand1 = operand1;}

    public ExtensionRegister getOperand2() {return operand2;}
    public void setOperand2(ExtensionRegister operand2) {this.operand2 = operand2;}

    /**
     * Compare to ZERO only!
     */
    public MCFPcompare(ExtensionRegister operand1) {super(TYPE.VCMP);this.operand1 = operand1;}
    public MCFPcompare(ExtensionRegister operand1, ExtensionRegister operand2) {super(TYPE.VCMP);this.operand1 = operand1;this.operand2 = operand2;}
}
