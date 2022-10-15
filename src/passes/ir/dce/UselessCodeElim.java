package passes.ir.dce;

import ir.Module;
import ir.Use;
import ir.Value;
import ir.values.BasicBlock;
import ir.values.Instruction;
import ir.values.instructions.CallInst;
import ir.values.instructions.GetElemPtrInst;
import ir.values.instructions.MemoryInst;
import ir.values.instructions.TerminatorInst;
import passes.ir.IRPass;

import java.util.*;

/**
 * <p>Remove all useless code.</p>
 *
 * <p>
 * All codes with no effect will be removed.
 * Note that we will judge whether a function has side effect, so that we can determine whether to remove a call instruction or not.
 * </p>
 */
public class UselessCodeElim implements IRPass {

    Map<ir.values.Function, Function> functionMap = new HashMap<>();

    @Override
    public synchronized void runOnModule(Module module) {
        // Create wrapped functions
        module.functions.forEach(function -> functionMap.put(function, new Function(function)));
        // Analysis side effect
        analysisSideEffect();
        // Optimize useless instructions
        functionMap.values().forEach(this::optimize);
        // Ready for next call
        functionMap.clear();
    }

    static boolean isLocalAddress(Value value) {
        if (value instanceof MemoryInst.Alloca) return true;
        if (value instanceof GetElemPtrInst) {
            var gep = (GetElemPtrInst) value;
            return isLocalAddress(gep.getOperandAt(0));
        }
        return false;
    }

    void analysisSideEffect() {
        Queue<Function> queue = new ArrayDeque<>();
        // Pre analysis
        for (Function function : functionMap.values()) {
            for (BasicBlock basicBlock : function.getRawFunction()) {
                for (Instruction instruction : basicBlock) {
                    if (instruction instanceof MemoryInst.Store) {
                        var storeInst = (MemoryInst.Store) instruction;
                        if (!isLocalAddress(storeInst.getOperandAt(1))) {
                            function.setSideEffect(true);
                        }
                    }
                    if (instruction instanceof CallInst) {
                        var callee = (ir.values.Function) instruction.getOperandAt(0);
                        if (callee.isExternal()) {
                            function.setSideEffect(true);
                        } else {
                            functionMap.get(callee).addCaller(function);
                        }
                    }
                }
            }
            if (function.hasSideEffect()) {
                queue.add(function);
            }
        }
        while (!queue.isEmpty()) {
            var curFunc = queue.remove();
            for (Function caller : curFunc.getCallers()) {
                if (!caller.hasSideEffect()) {
                    caller.setSideEffect(true);
                    queue.add(caller);
                }
            }
        }
    }

    boolean canSafelyRemove(Instruction instruction) {
        if (instruction.getUses().size() > 0) return false;
        if (instruction instanceof TerminatorInst) return false;
        if (instruction instanceof MemoryInst.Store) return false;
        if (instruction instanceof CallInst) {
            var callee = (ir.values.Function) instruction.getOperandAt(0);
            if (!functionMap.containsKey(callee)) return false;
            return !functionMap.get(callee).hasSideEffect();
        }
        return true;
    }

    void optimize(Function function) {
        Queue<Instruction> queue = new ArrayDeque<>();
        function.getRawFunction().forEach(basicBlock -> basicBlock.forEach(instruction -> {
            if (canSafelyRemove(instruction)) {
                queue.add(instruction);
            }
        }));
        while (!queue.isEmpty()) {
            Instruction instruction = queue.remove();
            @SuppressWarnings("unchecked")
            List<Use> operandList = (List<Use>) instruction.getOperands().clone();
            instruction.markWasted();
            operandList.forEach(use -> {
                var usee = use.getUsee();
                if (!(usee instanceof Instruction)) return;
                var nextInst = (Instruction) usee;
                if (canSafelyRemove(nextInst)) queue.add(nextInst);
            });
        }
    }

}
