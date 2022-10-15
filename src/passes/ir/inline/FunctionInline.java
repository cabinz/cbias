package passes.ir.inline;

import ir.Module;
import ir.Value;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.Instruction;
import ir.values.instructions.CallInst;
import ir.values.instructions.MemoryInst;
import ir.values.instructions.PhiInst;
import ir.values.instructions.TerminatorInst;
import passes.ir.IRPass;

import java.util.*;

/**
 * <p>Inline functions as much as possible.(Even it is bad to do so sometimes.)</p>
 * <p>It prefer to inline small functions to bigger ones.</p>
 */
public class FunctionInline implements IRPass {

    static class FunctionElement{
        private final Function function;

        private int size;

        public FunctionElement(Function function){
            this.function = function;
            this.size = evaluateSize(function);
        }

        public void reevaluate(){
            this.size = evaluateSize(this.function);
        }

        private static int evaluateSize(Function function){
            int size = 0;
            for (BasicBlock basicBlock : function) {
                size += 1;
                size += basicBlock.size();
            }
            return size;
        }

    }

    /**
     * Check whether a function can be inlined.
     * @param function The function to be checked.
     * @return True if the function is not recursive.
     */
    private static boolean canBeInlined(Function function){
        // Main cannot be inlined
        if(Objects.equals(function.getName(), "main")) return false;
        // Check whether it is recursive
        for (BasicBlock basicBlock : function) {
            for (Instruction instruction : basicBlock) {
                if(instruction instanceof CallInst){
                    var callInst = (CallInst) instruction;
                    var callee = (Function) callInst.getOperandAt(0);
                    if(callee==function) return false;
                }
            }
        }
        return true;
    }

    private static boolean isUpdated(FunctionElement functionElement){
        var oldSize = functionElement.size;
        functionElement.reevaluate();
        return oldSize != functionElement.size;
    }

    public void runOnModule(Module module){
        optimize(module);
    }

    private static void optimize(Module module){

        // Main process
        TreeSet<FunctionElement> functionSet = new TreeSet<>((lhs,rhs)->{
            if(lhs.size!=rhs.size){
                return Integer.compare(lhs.size, rhs.size);
            }else{
                return Integer.compare(lhs.hashCode(),rhs.hashCode());
            }
        });
        for (Function function : module.functions) {
            if(canBeInlined(function)){
                functionSet.add(new FunctionElement(function));
            }
        }
        while (!functionSet.isEmpty()){
            var element = functionSet.pollFirst();
            assert element != null; // Stupid JDK
            if(!canBeInlined(element.function)) continue;
            if(isUpdated(element)){
                functionSet.add(element);
                continue;
            }
            element.function.getUses().forEach(use -> {
                var callInst = (CallInst) use.getUser();
                extractCaller(callInst);
            });
            module.functions.remove(element.function);
        }
    }

    /**
     * Split the block of the call instruction.
     * @return The newly created block. (Containing instructions after call inst)
     */
    private static BasicBlock splitBlockByInst(Instruction instruction){
        BasicBlock prevBlock = instruction.getBB();
        BasicBlock followingBlock = new BasicBlock("INLINE_EXIT");
        boolean shouldPutToFollowing = false;
        for (Instruction prevBlockInstruction : prevBlock.getInstructions()) {
            if(prevBlockInstruction==instruction){
                shouldPutToFollowing = true;
            }
            if(shouldPutToFollowing){
                prevBlockInstruction.removeSelf();
                followingBlock.insertAtEnd(prevBlockInstruction);
            }
        }
        prevBlock.getUses().forEach(use -> {
            if(use.getUser() instanceof PhiInst){
                use.setUsee(followingBlock);
            }
        });
        return followingBlock;
    }

    private static void extractCaller(CallInst callInst){
        var function = callInst.getBB().getFunc();
        var callee = (Function) callInst.getOperandAt(0);
        var apNum = callInst.getNumOperands()-1;
        var aps = new ArrayList<Value>();
        for(int i=1;i<=apNum;i++){
            aps.add(callInst.getOperandAt(i));
        }
        var prevBlock = callInst.getBB();
        var followingBlock = splitBlockByInst(callInst);
        var clonedFunction = new ClonedFunction(callee, aps, followingBlock);

        prevBlock.insertAtEnd(new TerminatorInst.Br(clonedFunction.getEntryBlock()));
        for (BasicBlock basicBlock : clonedFunction.getBasicBlocks()) {
            function.addBB(basicBlock);
        }
        function.addBB(followingBlock);
        callInst.replaceSelfTo(clonedFunction.getExitValue());

        // Move alloca
        var allocas = new ArrayList<MemoryInst.Alloca>();
        for (Instruction instruction : clonedFunction.getEntryBlock()) {
            if(instruction instanceof MemoryInst.Alloca){
                allocas.add((MemoryInst.Alloca) instruction);
            }else{
                break;
            }
        }
        for (MemoryInst.Alloca alloca : allocas) {
            alloca.removeSelf();
            function.getEntryBB().insertAtFront(alloca);
        }

    }

}
