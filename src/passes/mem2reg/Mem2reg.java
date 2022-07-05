package passes.mem2reg;

import ir.Module;
import ir.values.instructions.MemoryInst;

import java.util.*;

public class Mem2reg{

    public static void optimize(Module module) {
        module.functions.forEach(Mem2reg::optimize);
    }

    /**
     * Run mem2reg on a single function.
     *
     * @param function The function to be optimized.
     */
    private static void optimize(ir.values.Function function){
        var wrappedFunction = new Function(function);
        var promotableVariables = wrappedFunction.getPromotableAllocaInstructions();

        collectNpdInformation(wrappedFunction,promotableVariables);


    }

    /**
     * Collect Need Previous Define(NPD) information for each block of the function.
     *
     * @param function A wrapped function which is being processed.
     * @param variables Variables to be promoted.
     */
    private static void collectNpdInformation(Function function, Collection<MemoryInst.Alloca> variables){
        /*
            Collect information of promotable variables in each block, containing:
            1. Variables changed in each block.
            2. Variables need to be defined in the previous block.
            3. Blocks that contains some specific variable that need to be defined in the previous block. (Spread later.)
         */
        Map<MemoryInst.Alloca, Queue<BasicBlock>> blocksToSpread = new HashMap<>();
        variables.forEach(variable -> blocksToSpread.put(variable, new LinkedList<>()));
        function.forEach(basicBlock -> basicBlock.forEach(instruction -> {
            if(instruction.isStore()){
                var address = instruction.getOperandAt(1);
                if(address instanceof MemoryInst.Alloca && variables.contains((MemoryInst.Alloca)address)){
                    basicBlock.changedVar.add((MemoryInst.Alloca)address);
                }
            }
            if(instruction.isLoad()){
                var address = instruction.getOperandAt(0);
                if(address instanceof MemoryInst.Alloca &&
                        variables.contains((MemoryInst.Alloca) address) &&
                        !basicBlock.changedVar.contains((MemoryInst.Alloca)address)
                ){
                    basicBlock.npdVar.add((MemoryInst.Alloca)address);
                    blocksToSpread.get((MemoryInst.Alloca)address).add(basicBlock);
                }
            }
        }));
        // Spread npd variables
        blocksToSpread.forEach((variable, basicBlocks) -> {
            while(!basicBlocks.isEmpty()){
                BasicBlock basicBlock = basicBlocks.remove();
                basicBlock.previousBasicBlocks.forEach(previousBasicBlock -> {
                    if(previousBasicBlock.changedVar.contains(variable) || previousBasicBlock.npdVar.contains(variable)) return;
                    previousBasicBlock.npdVar.add(variable);
                    basicBlocks.add(previousBasicBlock);
                });
            }
        });
    }

}
