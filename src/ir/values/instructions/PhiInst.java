package ir.values.instructions;

import ir.Type;
import ir.values.BasicBlock;
import ir.values.Instruction;

import java.util.HashMap;
import java.util.Map;

public class PhiInst extends Instruction {

    Map<BasicBlock, Instruction> phiMapping = new HashMap<>();

    public PhiInst(Type type, BasicBlock basicBlock){
        super(type, InstCategory.PHI, basicBlock);
    }

    public void setPhiMapping(Map<BasicBlock, Instruction> phiMapping){
        this.phiMapping = phiMapping;
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();
        builder.append("phi");

        final Boolean[] isFirstBranch = {true}; // Only in this cay can we change the value inside lambda.
        phiMapping.forEach((basicBlock, instruction) -> {
            builder.append(isFirstBranch[0] ?' ':',');
            builder.append(String.format("[%s,%s]",instruction.getName(),basicBlock.getName()));
            isFirstBranch[0] = false;
        });

        return builder.toString();
    }
}
