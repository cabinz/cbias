package passes.ir.hoist;

import ir.Module;
import ir.Use;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.Instruction;
import ir.values.instructions.*;
import passes.ir.IRPass;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

public class Hoist implements IRPass {
    @Override
    public void runOnModule(Module module) {
        for (Function function : module.functions) {
            optimize(function);
        }
    }

    static void optimize(Function function) {
        InstructionSet instructionSet = new InstructionSet();
        Queue<Instruction> uncheckedList = new ArrayDeque<>();
        for (BasicBlock basicBlock : function) {
            for (Instruction instruction : basicBlock) {
                uncheckedList.add(instruction);
            }
        }
        while (!uncheckedList.isEmpty()){
            var instruction = uncheckedList.remove();
            if(instruction.getBB()==null) return; // Already removed
            if (instruction instanceof UnaryOpInst || instruction instanceof BinaryOpInst ||
                    instruction instanceof CastInst || instruction instanceof PhiInst ||
                    instruction instanceof GetElemPtrInst) {
                if (instructionSet.contains(instruction)) {
                    var oldInst = instructionSet.get(instruction);
                    if(oldInst==instruction) continue; // Maybe pushed twice
                    @SuppressWarnings("unchecked")
                    var changedList = (List<Use>)instruction.getUses().clone();
                    instruction.replaceSelfTo(oldInst);
                    for (Use use : changedList) {
                        var user = use.getUser();
                        if(!(user instanceof Instruction)) continue;
                        uncheckedList.add((Instruction) user);
                    }
                }else{
                    instructionSet.add(instruction);
                }
            }
        }
    }

}
