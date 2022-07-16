package backend.armCode.MCInstructions;

import backend.armCode.MCInstruction;
import backend.operand.MCOperand;
import backend.operand.Register;

import java.util.HashSet;

public class MCcmp extends MCInstruction {

    private Register operand1;
    private MCOperand operand2;

    public Register getOperand1() {return operand1;}
    public void setOperand1(Register operand1) {this.operand1 = operand1;}

    public MCOperand getOperand2() {return operand2;}
    public void setOperand2(MCOperand operand2) {this.operand2 = operand2;}

    @Override
    public HashSet<Register> getUse() {
        var set = new HashSet<Register>();
        set.add(operand1);
        if (operand2.isVirtualReg() || operand2.isVirtualReg())
            set.add(((Register) operand2));
        if (shift != null && shift.getOperand().isVirtualReg())
            set.add(((Register) shift.getOperand()));
        return set;
    }

    @Override
    public HashSet<Register> getDef() {
        return new HashSet<>();
    }

    @Override
    public void replaceRegister(Register old, Register tmp) {
        if (operand1 == old) operand1 = tmp;
        if (operand2 == old) operand2 = tmp;
        if (shift != null && shift.getOperand() == old) shift.setRegister(tmp);
    }

    @Override
    public String emit() {
        return "CMP " + operand1.emit() + ", " + operand2.emit() + emitShift();
    }

    public MCcmp(Register operand1, MCOperand operand2) {super(TYPE.CMP);this.operand1 = operand1;this.operand2 = operand2;}
}
