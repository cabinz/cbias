package ir.values.instructions;

import ir.Type;
import ir.Value;
import ir.values.BasicBlock;
import ir.values.Constant;
import ir.values.Instruction;

import java.util.Map;
import java.util.Objects;

public class PhiInst extends Instruction {

    public PhiInst(Type type){
        super(type, InstCategory.PHI);
    }

    int nextMappingId = 0;
    Map<BasicBlock, Integer> operandMapping = new HashMap<>();

    public boolean hasEntry(){
        return getNumOperands()!=0;
    }

    public void addMapping(BasicBlock basicBlock, Value value){
        this.addOperandAt(value,nextMappingId);
        this.addOperandAt(basicBlock,nextMappingId+1);
        operandMapping.put(basicBlock, nextMappingId);
        nextMappingId += 2;
    }

    public void removeMapping(BasicBlock basicBlock){
        if(!operandMapping.containsKey(basicBlock)){
            throw new RuntimeException("Trying to remove a mapping that does not exists");
        }
        int id = operandMapping.get(basicBlock);
        this.removeOperandAt(id);
        this.removeOperandAt(id+1);
    }

    public void setPhiMapping(Map<BasicBlock, Value> phiMapping){
        phiMapping.forEach(this::addMapping);
    }

    public boolean isConstant(){
        Value stdValue = null;
        for (Integer id : operandMapping.values()) {
            var value = getOperandAt(id);
            if(stdValue==null){
                stdValue = value;
            }else{
                if(!Objects.equals(stdValue,value)) return false;
            }
        }
        return true;
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
