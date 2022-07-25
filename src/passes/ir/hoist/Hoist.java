package passes.ir.hoist;

import ir.Module;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.Instruction;
import ir.values.instructions.*;
import passes.ir.IRPass;

import java.util.List;

public class Hoist implements IRPass {
    @Override
    public void runOnModule(Module module) {
        for (Function function : module.functions) {
            for (BasicBlock basicBlock : function) {
                optimize(basicBlock);
            }
        }
    }

    static void optimize(BasicBlock basicBlock) {
        InstructionSet instructionSet = new InstructionSet();
        @SuppressWarnings("unchecked")
        var instructions = (List<Instruction>) basicBlock.instructions.clone();
        for (Instruction instruction : instructions) {
            if (instruction instanceof UnaryOpInst || instruction instanceof BinaryOpInst ||
                    instruction instanceof CastInst || instruction instanceof PhiInst ||
                    instruction instanceof GetElemPtrInst) {
                if (instructionSet.contains(instruction)) {
                    var oldInst = instructionSet.get(instruction);
                    instruction.replaceSelfTo(oldInst);
                }else{
                    instructionSet.add(instruction);
                }
            }
        }
    }

}
