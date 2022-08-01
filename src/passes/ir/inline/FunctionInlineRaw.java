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

import java.util.*;

public class FunctionInlineRaw {

    static class FunctionElement implements Comparable<FunctionElement>{
        private final Function function;

        private int size;

        public FunctionElement(Function function){
            this.function = function;
            this.size = evaluateSize(function);
        }

        public void reevaluate(){
            this.size = evaluateSize(this.function);
        }

        @Override
        public int compareTo(FunctionElement rhs) {
            return Integer.compare(size, rhs.size);
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
     * Check weather a function can be inlined.
     * @param function The function to be checked.
     * @return True if the function is not recursive.
     */
    private static boolean canBeInlined(Function function){
        // Main cannot be inlined
        if(Objects.equals(function.getName(), "main")) return false;
        // Check weather it is recursive
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

    public void optimize(Module module){

        // Main process
        TreeSet<FunctionElement> functionSet = new TreeSet<>();
        for (Function function : module.functions) {
            if(canBeInlined(function)){
                functionSet.add(new FunctionElement(function));
            }
        }
        while (!functionSet.isEmpty()){
            var element = functionSet.pollFirst();
            assert element != null; // Stupid JDK
            element.function.getUses().forEach(use -> extractCaller((CallInst) use.getUser()));
            module.functions.remove(element.function);
        }
    }

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
                var phiInst = (PhiInst) use.getUser();
                var value = phiInst.findValue(prevBlock);
                phiInst.removeMapping(prevBlock);
                phiInst.addMapping(followingBlock,value);
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
