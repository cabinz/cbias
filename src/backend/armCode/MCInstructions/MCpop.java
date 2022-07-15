package backend.armCode.MCInstructions;

import backend.armCode.MCInstruction;
import backend.operand.Register;

import java.util.ArrayList;
import java.util.HashSet;

public class MCpop extends MCInstruction{
    private ArrayList<Register> operands;

    @Override
    public HashSet<Register> getUse() {
        return new HashSet<>();
    }

    @Override
    public HashSet<Register> getDef() {
        return new HashSet<>(operands);
    }

    @Override
    public void replaceRegister(Register old, Register tmp) {}

    @Override
    public String emit() {
        String ret = "POP {";
        for (Register r : operands){
            ret += r.emit() + ", ";
        }
        return ret.substring(0,ret.length()-2) + "}";
    }

    public ArrayList<Register> getOperands() {return operands;}

    public MCpop(Register op1) {super(TYPE.POP);operands = new ArrayList<>();operands.add(op1);}
    public MCpop(Register op1, Register op2) {super(TYPE.POP);operands = new ArrayList<>();operands.add(op1);operands.add(op2);}
    public MCpop(Register op1, Register op2, Register op3) {super(TYPE.POP);operands = new ArrayList<>();operands.add(op1);operands.add(op2);operands.add(op3);}
    public MCpop(Register op1, Register op2, Register op3, Register op4) {super(TYPE.POP);operands = new ArrayList<>();operands.add(op1);operands.add(op2);operands.add(op3);operands.add(op4);}
    public MCpop(ArrayList<Register> operands) {super(TYPE.POP); this.operands = operands;}
}