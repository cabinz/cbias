package ir.values.instructions;

import ir.Type;
import ir.Use;
import ir.Value;
import ir.values.BasicBlock;
import ir.values.Instruction;

import java.util.HashMap;
import java.util.Map;

public class PhiInst extends Instruction {

    public PhiInst(Type type, BasicBlock basicBlock){
        super(type, InstCategory.PHI, basicBlock);
    }

    public void setPhiMapping(Map<BasicBlock, Value> phiMapping){
        var id = 0;
        for (BasicBlock basicBlock : phiMapping.keySet()) {
            this.addOperandAt(phiMapping.get(basicBlock),id++);
            this.addOperandAt(basicBlock,id++);
        }
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();
        builder.append(this.getName());
        builder.append(" = ");
        builder.append("phi ");
        builder.append(this.getType().toString());

        boolean isFirstBranch = true;
        for(int i=0;i<operands.size();i+=2){
            builder.append(isFirstBranch ?' ':',');
            builder.append(String.format("[%s,%s]",getOperandAt(i).getName(),"%"+getOperandAt(i+1).getName()));
            isFirstBranch = false;
        }

        return builder.toString();
    }
}
