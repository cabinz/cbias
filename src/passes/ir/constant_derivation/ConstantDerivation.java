package passes.ir.constant_derivation;

import ir.Module;
import ir.Use;
import ir.Value;
import ir.values.*;
import ir.values.constants.ConstFloat;
import ir.values.constants.ConstInt;
import ir.values.instructions.*;
import passes.ir.IRPass;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * This optimization includes the following passes: <br />
 * 1) Derive global constant.
 * 2) Derive const instruction.
 * 3) Derive const branch condition.
 */
public class ConstantDerivation implements IRPass {

    @Override
    public void runOnModule(Module module) {
        optimize(module);
    }

    static void optimize(Module module){
        deriveConstantGV(module);
        module.functions.forEach(ConstantDerivation::deriveConstantInstruction);
    }

    /// Derive global constant.

    static boolean isGlobalConstant(GlobalVariable variable){
        for(Use use: variable.getUses()){
            if(!(use.getUser() instanceof MemoryInst.Load)){
                return false;
            }
        }
        return true;
    }

    static void deriveConstantGV(Module module){
        @SuppressWarnings("unchecked")
        var globalVariableList = (List<GlobalVariable>) module.globalVariables.clone();
        globalVariableList.forEach(globalVariable -> {
            if(isGlobalConstant(globalVariable)){
                var constant = globalVariable.getInitVal();
                globalVariable.getUses().forEach(loadUse -> {
                    var loadInst = (MemoryInst.Load) loadUse.getUser();
                    @SuppressWarnings("unchecked")
                    var uses = (List<Use>) loadInst.getUses().clone();
                    uses.forEach(use -> use.setUsee(constant));
                    loadInst.removeSelf();
                });
                module.globalVariables.remove(globalVariable);
            }
        });
    }

    /**
     * Judge weather the value is a constant.
     * @param value The value to be judged.
     * @return True if value is instance of: <br>
     *  - BasicBlock <br>
     *  - Constant <br>
     *  - Function <br>
     */
    static boolean isConstant(Value value){
        if(value instanceof BasicBlock) return true;
        if(value instanceof Constant) return true;
        if(value instanceof Function) return true;
        return false;
    }

    /**
     * Judge weather an instruction can be derived.
     * @param instruction The instruction to be judged.
     */
    static boolean canDeriveInst(Instruction instruction){
        for (Use use : instruction.operands) {
            if(!isConstant(use.getUsee())) return false;
        }
        if(instruction instanceof PhiInst){
            return instruction.operands.size() <= 2; //One branch has 2 operands
        }else if(instruction instanceof CallInst){
            return false;
        }else{
            return true;
        }
    }

    static Constant calculateInstValue(Instruction instruction){
        switch (instruction.cat) {
            case ADD, SUB, MUL, DIV -> {
                var c1 = (ConstInt) instruction.getOperandAt(0);
                var c2 = (ConstInt) instruction.getOperandAt(1);
                switch (instruction.cat){
                    case ADD -> {
                        return ConstInt.getI32(c1.getVal()+c2.getVal());
                    }
                    case SUB -> {
                        return ConstInt.getI32(c1.getVal()- c2.getVal());
                    }
                    case MUL -> {
                        return ConstInt.getI32(c1.getVal()* c2.getVal());
                    }
                    case DIV -> {
                        return ConstInt.getI32(c1.getVal()/ c2.getVal());
                    }
                }
            }
            case FADD, FSUB, FMUL, FDIV -> {
                var c1 = (ConstFloat) instruction.getOperandAt(0);
                var c2 = (ConstFloat) instruction.getOperandAt(1);
                switch (instruction.cat){
                    case FADD -> {
                        return ConstFloat.get(c1.getVal()+c2.getVal());
                    }
                    case FSUB -> {
                        return ConstFloat.get(c1.getVal()- c2.getVal());
                    }
                    case FMUL -> {
                        return ConstFloat.get(c1.getVal()* c2.getVal());
                    }
                    case FDIV -> {
                        return ConstFloat.get(c1.getVal()/ c2.getVal());
                    }
                }
            }
            case FNEG -> {
                var c1 = (ConstFloat) instruction.getOperandAt(0);
                return ConstFloat.get(-c1.getVal());
            }
            case LT, GT, EQ, NE, LE, GE -> {

            }
            case FLT, FGT, FEQ, FNE, FLE, FGE -> {

            }
            case PHI -> {
                return (Constant) instruction.getOperandAt(0);
            }
        }
        return ConstInt.getI32(0); //Temp code, avoid CE
    }

    static void deriveConstantInstruction(Function function){
        Queue<Instruction> queue = new ArrayDeque<>();
        for (BasicBlock basicBlock : function) {
            for (Instruction instruction : basicBlock) {
                if(canDeriveInst(instruction)){
                    queue.add(instruction);
                }
            }
        }
        while(!queue.isEmpty()){
            Instruction instruction = queue.remove();
            if(instruction.getType().isVoidType()){
                // Br, Load, Store, etc.
                if(instruction instanceof TerminatorInst.Br br){
                    if(br.isCondJmp()){
                        // Require i1 constant
                    }
                }
            }else{
                // Derive constant value
                // Replace usage
            }
        }
    }

}
