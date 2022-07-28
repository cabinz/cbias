package passes.ir.mem2reg;

import ir.Module;
import ir.Use;
import ir.Value;
import ir.values.Constant;
import ir.values.Instruction;
import ir.values.constants.ConstFloat;
import ir.values.constants.ConstInt;
import ir.values.instructions.MemoryInst;
import ir.values.instructions.PhiInst;
import passes.PassManager;
import passes.ir.IRPass;
import passes.ir.dce.UnreachableCodeElim;

import java.util.*;

/**
 * Mem2reg optimize
 */
public class Mem2reg implements IRPass {

    @Override
    public void runOnModule(Module module) {
        Mem2reg.optimize(module);
    }

    static void optimize(Module module) {
        PassManager.getInstance().run(UnreachableCodeElim.class, module);
        module.functions.forEach(Mem2reg::optimize);
    }

    /**
     * Run mem2reg on a single function.
     *
     * @param function The function to be optimized.
     */
    static void optimize(ir.values.Function function){
        var wrappedFunction = new Function(function);
        var promotableVariables = wrappedFunction.getPromotableAllocaInstructions();

        collectUDInformation(wrappedFunction,promotableVariables);
        wrappedFunction.forEach(Mem2reg::insertEmptyPhi);
        wrappedFunction.forEach(Mem2reg::processInstructions);
        wrappedFunction.forEach(Mem2reg::fillEmptyPhi);
        wrappedFunction.forEach(basicBlock -> Mem2reg.removeLoadStoreInst(basicBlock,promotableVariables));
        Mem2reg.removeAllocaInst(wrappedFunction, promotableVariables);
    }

    /**
     * Collect use and define information for each block.
     *
     * @param function A wrapped function which is being processed.
     * @param variables Variables to be promoted.
     */
    static void collectUDInformation(Function function, Collection<MemoryInst.Alloca> variables){
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
                    basicBlock.definedVar.add((MemoryInst.Alloca)address);
                }
            }
            if(instruction.isLoad()){
                var address = instruction.getOperandAt(0);
                if(address instanceof MemoryInst.Alloca &&
                        variables.contains((MemoryInst.Alloca) address) &&
                        !basicBlock.definedVar.contains((MemoryInst.Alloca)address)
                ){
                    basicBlock.npdVar.add((MemoryInst.Alloca)address);
                    blocksToSpread.get((MemoryInst.Alloca)address).add(basicBlock);
                }
            }
        }));
        // Spread need previous define(NPD) variables
        blocksToSpread.forEach((variable, basicBlocks) -> {
            while(!basicBlocks.isEmpty()){
                BasicBlock basicBlock = basicBlocks.remove();
                basicBlock.previousBasicBlocks.forEach(previousBasicBlock -> {
                    if(previousBasicBlock.definedVar.contains(variable) || previousBasicBlock.npdVar.contains(variable)) return;
                    previousBasicBlock.npdVar.add(variable);
                    basicBlocks.add(previousBasicBlock);
                });
            }
        });
    }

    /**
     * Insert PHI instruction to the beginning of that block.
     *
     * @param basicBlock The block which needs to insert PHI.
     */
    static void insertEmptyPhi(BasicBlock basicBlock){
        if(basicBlock.previousBasicBlocks.size()==0){
            /* Entry block should not contain phi inst.
             * If the size of npdVar is not 0, it causes an undefined-behavior.
             * The code may be well-defined, but if it contains complex branches, the compiler cannot find out so.
             * In this case, we just assume the initial value is 0.
             */
            basicBlock.npdVar.forEach(npdVar -> {
                Constant constant;
                if(npdVar.getAllocatedType().isIntegerType()){
                    constant = ConstInt.getI32(0);
                }else if(npdVar.getAllocatedType().isFloatType()){
                    constant = ConstFloat.get(0f);
                }else{
                    throw new RuntimeException("Unable to generate default value for type "+npdVar.getAllocatedType());
                }
                basicBlock.latestDefineMap.put(npdVar,constant);
            });
        }else{
            basicBlock.npdVar.forEach( npdVar -> {
                var phiInst = new PhiInst(npdVar.getAllocatedType());
                basicBlock.getRawBasicBlock().insertAtFront(phiInst);
                basicBlock.importPhiMap.put(npdVar,phiInst);
                basicBlock.latestDefineMap.put(npdVar,phiInst);
            });
        }
    }

    /**
     * Process instructions inside the block. This function will: <br />
     *
     * 1. Remove load/store/alloca instructions, replace the usage of their result with registers. <br />
     * 2. Fill in the latestDefineMap, which will be used in {@link Mem2reg#fillEmptyPhi(BasicBlock)}.  <br />
     *
     * @param basicBlock The basic block to be processed.
     */
    static void processInstructions(BasicBlock basicBlock){
        basicBlock.forEach(instruction -> {
            // Replace all usage of 'load' to register
            if(instruction instanceof MemoryInst.Load) {
                var address = instruction.getOperandAt(0);
                if(address instanceof MemoryInst.Alloca && basicBlock.latestDefineMap.containsKey(address)){
                    var register = basicBlock.latestDefineMap.get(address);
                    instruction.getUses().forEach(use -> use.setUsee(register));
                }
            }
            // Record 'store' instruction
            if(instruction instanceof MemoryInst.Store){
                var value = instruction.getOperandAt(0);
                var address = instruction.getOperandAt(1);
                if(address instanceof MemoryInst.Alloca){
                    basicBlock.latestDefineMap.put((MemoryInst.Alloca) address,value);
                }
            }
        });
    }

    /**
     * Finish the declaration of PHI instructions.
     *
     * @param basicBlock The block whose PHI instruction is empty.
     */
    static void fillEmptyPhi(BasicBlock basicBlock){
        basicBlock.importPhiMap.forEach((alloca, phiInst) -> {
            var map = new HashMap<ir.values.BasicBlock, Value>();
            basicBlock.previousBasicBlocks.forEach(previousBasicBlock ->
                    map.put(previousBasicBlock.getRawBasicBlock(), previousBasicBlock.latestDefineMap.get(alloca))
            );
            phiInst.setPhiMapping(map);
        });
    }

    static void removeLoadStoreInst(BasicBlock basicBlock, Collection<MemoryInst.Alloca> variables){
        // 'instructions' is changed during the following 'forEach', so we must clone one.
        @SuppressWarnings("unchecked")
        var instructions = (List<Instruction>) basicBlock.getRawBasicBlock().instructions.clone();
        instructions.forEach(instruction -> {
            if(instruction instanceof MemoryInst.Load){
                var address = instruction.getOperandAt(0);
                if(address instanceof MemoryInst.Alloca && variables.contains(address)){
                    instruction.removeSelf();
                }
            }
            if(instruction instanceof MemoryInst.Store){
                var address = instruction.getOperandAt(1);
                if(address instanceof MemoryInst.Alloca && variables.contains(address)){
                    instruction.removeSelf();
                }
            }
        });
    }

    static void removeAllocaInst(Function function, Collection<MemoryInst.Alloca> variables){
        var entryBlock = function.getRawFunction().getEntryBB();
        @SuppressWarnings("unchecked")
        var instructions = (List<Instruction>)entryBlock.instructions.clone();
        instructions.forEach(instruction -> {
            if(instruction instanceof MemoryInst.Alloca && variables.contains(instruction)){
                instruction.removeSelf();
            }
        });
    }

}
