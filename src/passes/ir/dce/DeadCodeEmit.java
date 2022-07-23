package passes.ir.dce;

import ir.Module;
import ir.values.Function;
import ir.values.Instruction;
import ir.values.instructions.CallInst;
import ir.values.instructions.MemoryInst;
import ir.values.instructions.TerminatorInst;
import passes.ir.IRPass;

import java.util.ArrayDeque;
import java.util.Queue;

public class DeadCodeEmit implements IRPass {

    @Override
    public void runOnModule(Module module) {
        module.functions.forEach(DeadCodeEmit::optimize);
    }

    static boolean canSafelyRemove(Instruction instruction){
        return !(instruction instanceof MemoryInst.Store||instruction instanceof TerminatorInst||instruction instanceof CallInst);
    }

    static void optimize(Function function) {
        Queue<Instruction> queue = new ArrayDeque<>();
        function.forEach(basicBlock -> basicBlock.forEach(instruction -> {
            if (!canSafelyRemove(instruction)) {
                return;
            }
            if(instruction.getUses().size()==0){
                queue.add(instruction);
            }
        }));
        while (!queue.isEmpty()){
            Instruction instruction = queue.remove();
            //todo:
            instruction.removeSelf();
        }
    }

}
