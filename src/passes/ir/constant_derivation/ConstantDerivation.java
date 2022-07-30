package passes.ir.constant_derivation;

import ir.Module;
import ir.Use;
import ir.Value;
import ir.values.*;
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
                // Br, Load, Store, etc.
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
        for (Instruction instruction : basicBlock.getRawBasicBlock()) {
            if (instruction instanceof PhiInst) {
                var phiInst = (PhiInst) instruction;
                phiInst.removeMapping(entry.getRawBasicBlock());
                if (phiInst.canDerive()) {
                    deriveQueue.add(phiInst);
                }
            } else {
                break; // PHI must be in the front of a bb
            }
        }
        basicBlock.prevBlocks.remove(entry);
        if (basicBlock.prevBlocks.size()==0){
            basicBlock.getRawBasicBlock().removeSelf();
            var followingBBs = basicBlock.followingBlocks;
            for (BasicBlock followingBlock : followingBBs) {
                removeEntry(followingBlock, basicBlock, deriveQueue);
            }
        }
    }

}
