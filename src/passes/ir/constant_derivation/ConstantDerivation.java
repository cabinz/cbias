package passes.ir.constant_derivation;

import ir.Module;
import ir.Use;
import ir.Value;
import ir.types.PointerType;
import ir.values.*;
import ir.values.constants.ConstArray;
import ir.values.constants.ConstFloat;
import ir.values.constants.ConstInt;
import ir.values.instructions.*;
import passes.ir.IRPass;
import passes.ir.RelationAnalysis;

import java.util.*;

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

    static void optimize(Module module) {
        deriveConstantGV(module);
        module.functions.forEach(ConstantDerivation::deriveConstantExpression);
    }

    /// Derive global constant.

    static boolean isGlobalConstant(GlobalVariable variable) {
        for (Use use : variable.getUses()) {
            if (!(use.getUser() instanceof MemoryInst.Load)) {
                return false;
            }
        }
        return true;
    }

    static void deriveConstantGV(Module module) {
        @SuppressWarnings("unchecked")
        var globalVariableList = (List<GlobalVariable>) module.getGlobalVariables().clone();
        globalVariableList.forEach(globalVariable -> {
            if (isGlobalConstant(globalVariable)) {
                var constant = globalVariable.getInitVal();
                globalVariable.getUses().forEach(loadUse -> {
                    var loadInst = (MemoryInst.Load) loadUse.getUser();
                    loadInst.replaceSelfTo(constant);
                });
                module.getGlobalVariables().remove(globalVariable);
            }
        });
    }

    /**
     * Judge weather the value is a constant.
     *
     * @param value The value to be judged.
     * @return True if value is instance of: <br>
     * - BasicBlock <br>
     * - Constant <br>
     * - Function <br>
     */
    static boolean isConstant(Value value) {
        if (value instanceof ir.values.BasicBlock) return true;
        if (value instanceof Constant) return true;
        if (value instanceof Function) return true;
        return false;
    }

    /**
     * Judge weather an expression can be derived.
     *
     * @param expression The expression to be judged.
     */
    static boolean canDeriveExpression(Instruction expression) {
        // Removed expression doesn't need to be derived.
        if(expression.getBB()==null){
            return false;
        }
        // Derive const array
        /* Actually, we should do this by creating 'const pointer', but there is no such thing in our code. So we can
         * only derive in this way.
         */
        if (expression instanceof MemoryInst.Load){
            if(!(expression.getOperandAt(0) instanceof GetElemPtrInst)) return false;
            var gep = (GetElemPtrInst) expression.getOperandAt(0);
            if(!(gep.getOperandAt(2) instanceof ConstInt)) return false;
            var retType = (PointerType) gep.getType();
            if(!(retType.getPointeeType().isIntegerType()|| retType.getPointeeType().isFloatType())) return false;
            while (gep.getOperandAt(0) instanceof GetElemPtrInst){
                gep = (GetElemPtrInst) gep.getOperandAt(0);
                if(!(gep.getOperandAt(2) instanceof ConstInt)) return false;
            }
            var target = gep.getOperandAt(0);
            if(target instanceof GlobalVariable){
                var gv = (GlobalVariable) target;
                //if(gv.isConstant()) return true;
            }
            return false;
        }
        // Call and MemoryInst returns void
        if (expression instanceof CallInst || expression instanceof MemoryInst) {
            return false;
        }
        // PHI can be derived without constant operands
        if (expression instanceof PhiInst) {
            var phiInst = (PhiInst) expression;
            return phiInst.canDerive();
        }
        // Other instructions must have constant operands
        for (Use use : expression.getOperands()) {
            if (!isConstant(use.getUsee())) return false;
        }
        return true;
    }

    static Value runGep(ConstArray array, GetElemPtrInst gep){
        if(array==null){
            var retType = ((PointerType) gep.getType()).getPointeeType();
            if(retType.isArrayType()) return null;
            if(retType.isIntegerType()) return ConstInt.getI32(0);
            if(retType.isFloatType()) return ConstFloat.get(0);
            throw new RuntimeException("Unknown return type "+gep.getType());
        }
        Value result = array;
        for(int i=2;i<gep.getNumOperands();i++){ // I don't know why it starts from 2
            var index = ((ConstInt)gep.getOperandAt(i)).getVal();
            result = ((ConstArray)result).getOperandAt(index);
        }
        return result;
    }

    static Value deriveGepValue(GetElemPtrInst gep){
        var target = gep.getOperandAt(0);
        if(target instanceof GetElemPtrInst){
            return runGep((ConstArray) deriveGepValue((GetElemPtrInst) target), gep);
        }
        if(target instanceof GlobalVariable){
            var gv = (GlobalVariable) target;
            if(gv.isConstant()){
                return runGep((ConstArray) gv.getInitVal(), gep);
            }
        }
        throw new RuntimeException("Cannot derive gep with operand[0]="+target);
    }

    static Value calculateExpressionValue(Instruction expression) {
        switch (expression.getTag()) {
            case ADD, SUB, MUL, DIV -> {
                var c1 = (ConstInt) expression.getOperandAt(0);
                var c2 = (ConstInt) expression.getOperandAt(1);
                switch (expression.getTag()) {
                    case ADD -> {
                        return ConstInt.getI32(c1.getVal() + c2.getVal());
                    }
                    case SUB -> {
                        return ConstInt.getI32(c1.getVal() - c2.getVal());
                    }
                    case MUL -> {
                        return ConstInt.getI32(c1.getVal() * c2.getVal());
                    }
                    case DIV -> {
                        return ConstInt.getI32(c1.getVal() / c2.getVal());
                    }
                }
            }
            case FADD, FSUB, FMUL, FDIV -> {
                var c1 = (ConstFloat) expression.getOperandAt(0);
                var c2 = (ConstFloat) expression.getOperandAt(1);
                switch (expression.getTag()) {
                    case FADD -> {
                        return ConstFloat.get(c1.getVal() + c2.getVal());
                    }
                    case FSUB -> {
                        return ConstFloat.get(c1.getVal() - c2.getVal());
                    }
                    case FMUL -> {
                        return ConstFloat.get(c1.getVal() * c2.getVal());
                    }
                    case FDIV -> {
                        return ConstFloat.get(c1.getVal() / c2.getVal());
                    }
                }
            }
            case FNEG -> {
                var c1 = (ConstFloat) expression.getOperandAt(0);
                return ConstFloat.get(-c1.getVal());
            }
            case LT, GT, EQ, NE, LE, GE -> {
                var c1 = (ConstInt) expression.getOperandAt(0);
                var c2 = (ConstInt) expression.getOperandAt(1);
                switch (expression.getTag()) {
                    case LT -> {
                        return ConstInt.getI1(c1.getVal() < c2.getVal() ? 1 : 0);
                    }
                    case GT -> {
                        return ConstInt.getI1(c1.getVal() > c2.getVal() ? 1 : 0);
                    }
                    case EQ -> {
                        return ConstInt.getI1(c1.getVal() == c2.getVal() ? 1 : 0);
                    }
                    case NE -> {
                        return ConstInt.getI1(c1.getVal() != c2.getVal() ? 1 : 0);
                    }
                    case LE -> {
                        return ConstInt.getI1(c1.getVal() <= c2.getVal() ? 1 : 0);
                    }
                    case GE -> {
                        return ConstInt.getI1(c1.getVal() >= c2.getVal() ? 1 : 0);
                    }
                }
            }
            case FLT, FGT, FEQ, FNE, FLE, FGE -> {
                var c1 = (ConstFloat) expression.getOperandAt(0);
                var c2 = (ConstFloat) expression.getOperandAt(1);
                switch (expression.getTag()) {
                    case FLT -> {
                        return ConstInt.getI1(c1.getVal() < c2.getVal() ? 1 : 0);
                    }
                    case FGT -> {
                        return ConstInt.getI1(c1.getVal() > c2.getVal() ? 1 : 0);
                    }
                    case FEQ -> {
                        return ConstInt.getI1(c1.getVal() == c2.getVal() ? 1 : 0);
                    }
                    case FNE -> {
                        return ConstInt.getI1(c1.getVal() != c2.getVal() ? 1 : 0);
                    }
                    case FLE -> {
                        return ConstInt.getI1(c1.getVal() <= c2.getVal() ? 1 : 0);
                    }
                    case FGE -> {
                        return ConstInt.getI1(c1.getVal() >= c2.getVal() ? 1 : 0);
                    }
                }
            }
            case AND, OR -> {
                var c1 = (ConstInt) expression.getOperandAt(0);
                var c2 = (ConstInt) expression.getOperandAt(0);
                switch (expression.getTag()) {
                    case AND -> {
                        return ConstInt.getI1(c1.getVal() & c2.getVal());
                    }
                    case OR -> {
                        return ConstInt.getI1(c1.getVal() | c2.getVal());
                    }
                }
            }
            case ZEXT, FPTOSI, SITOFP -> {
                switch (expression.getTag()) {
                    case ZEXT -> {
                        var c1 = (ConstInt) expression.getOperandAt(0);
                        return ConstInt.getI32(c1.getVal());
                    }
                    case FPTOSI -> {
                        var c1 = (ConstFloat) expression.getOperandAt(0);
                        return ConstInt.getI32((int) c1.getVal());
                    }
                    case SITOFP -> {
                        var c1 = (ConstInt) expression.getOperandAt(0);
                        return ConstFloat.get((float) c1.getVal());
                    }
                }
            }
            case PHI -> {
                return ((PhiInst) expression).deriveConstant();
            }
            case LOAD -> {
                var o0 = expression.getOperandAt(0);
                if(!(o0 instanceof GetElemPtrInst)){
                    throw new RuntimeException("Cannot derive with o0="+o0);
                }
                return deriveGepValue((GetElemPtrInst)o0);
            }
        }
        throw new RuntimeException("Unable to derive expression of type " + expression.getTag());
    }

    static void deriveConstantExpression(Function function) {
        Map<ir.values.BasicBlock, BasicBlock> basicBlockMap = new HashMap<>();
        for (ir.values.BasicBlock basicBlock : function) {
            basicBlockMap.put(basicBlock, new BasicBlock(basicBlock));
        }
        RelationAnalysis.analysisBasicBlocks(basicBlockMap);

        Queue<Instruction> queue = new ArrayDeque<>();
        for (ir.values.BasicBlock basicBlock : function) {
            for (Instruction instruction : basicBlock) {
                if (canDeriveExpression(instruction)) {
                    queue.add(instruction);
                }
            }
        }
        while (!queue.isEmpty()) {
            Instruction expression = queue.remove();

            if(!canDeriveExpression(expression)) continue;

            if (expression.getType().isVoidType()) {
                // Br, Store, etc.
                if (expression instanceof TerminatorInst.Br) {
                    var br = (TerminatorInst.Br) expression;
                    optimizeBr(basicBlockMap, queue, br);
                }
            } else {
                Value value = calculateExpressionValue(expression);
                var changeList = expression.getUses();
                expression.replaceSelfTo(value);
                for (Use use : changeList) {
                    var user = use.getUser();
                    if (!(user instanceof Instruction)) continue;
                    var inst = (Instruction) user;
                    if(canDeriveExpression(inst)){
                        queue.add(inst);
                    }
                }
            }
        }
    }

    private static void optimizeBr(Map<ir.values.BasicBlock, BasicBlock> basicBlockMap, Queue<Instruction> deriveQueue, TerminatorInst.Br br) {
        if (!br.isCondJmp()) return;
        var cond_ = br.getOperandAt(0);
        if (!(cond_ instanceof ConstInt)) return;
        var cond = (ConstInt) cond_;
        var bTrue = (ir.values.BasicBlock) br.getOperandAt(1);
        var bFalse = (ir.values.BasicBlock) br.getOperandAt(2);
        br.removeOperandAt(0);
        br.removeOperandAt(1);
        br.removeOperandAt(2);
        if (cond.getVal() == 1) {
            br.addOperandAt(0, bTrue);
            removeEntry(basicBlockMap.get(bFalse), basicBlockMap.get(br.getBB()), deriveQueue);
        } else {
            br.addOperandAt(0, bFalse);
            removeEntry(basicBlockMap.get(bTrue), basicBlockMap.get(br.getBB()), deriveQueue);
        }
    }

    // Special way of removing designed for ConstantDerivation

    /**
     * Remove one entry of a block.
     *
     * @param basicBlock Basic block one of whose entry is removed.
     * @param entry      The entry to be removed.
     */
    static void removeEntry(BasicBlock basicBlock, BasicBlock entry, Queue<Instruction> deriveQueue) {
        RelationAnalysis.removeEntry(basicBlock.getRawBasicBlock(), entry.getRawBasicBlock());
        for (Instruction instruction : basicBlock.getRawBasicBlock()) {
            if (instruction instanceof PhiInst) {
                var phiInst = (PhiInst) instruction;
                if (phiInst.canDerive()) {
                    deriveQueue.add(phiInst);
                }
            } else {
                break; // PHI must be in the front of a bb
            }
        }
        basicBlock.prevBlocks.remove(entry);
        entry.followingBlocks.remove(basicBlock);
        if (basicBlock.prevBlocks.size()==0){
            // Free operands to avoid deadlock
            RelationAnalysis.freeBlock(basicBlock.getRawBasicBlock());
            // Cascade remove other block
            var iterator = basicBlock.followingBlocks.iterator();
            while (iterator.hasNext()){
                var followingBlock = iterator.next();
                iterator.remove();
                removeEntry(followingBlock, basicBlock, deriveQueue);
            }
            // Totally clear self
            basicBlock.getRawBasicBlock().markWasted(); //markWasted
        }
    }

}
