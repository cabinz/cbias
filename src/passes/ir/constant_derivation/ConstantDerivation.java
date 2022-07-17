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
        var globalVariableList = (List<GlobalVariable>) module.globalVariables.clone();
        globalVariableList.forEach(globalVariable -> {
            if (isGlobalConstant(globalVariable)) {
                var constant = globalVariable.getInitVal();
                @SuppressWarnings("unchecked")
                var gvUses = (List<Use>) globalVariable.getUses().clone();
                gvUses.forEach(loadUse -> {
                    var loadInst = (MemoryInst.Load) loadUse.getUser();
                    @SuppressWarnings("unchecked")
                    var loadUses = (List<Use>) loadInst.getUses().clone();
                    loadUses.forEach(use -> use.setUsee(constant));
                    loadInst.removeSelf();
                });
                module.globalVariables.remove(globalVariable);
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
        if (value instanceof BasicBlock) return true;
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
        for (Use use : expression.operands) {
            if (!isConstant(use.getUsee())) return false;
        }
        if (expression instanceof PhiInst phiInst) {
            return phiInst.isConstant();
        } else if (expression instanceof CallInst || expression instanceof MemoryInst) {
            return false;
        } else {
            return true;
        }
    }

    static Constant calculateExpressionValue(Instruction expression) {
        switch (expression.cat) {
            case ADD, SUB, MUL, DIV -> {
                var c1 = (ConstInt) expression.getOperandAt(0);
                var c2 = (ConstInt) expression.getOperandAt(1);
                switch (expression.cat) {
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
                switch (expression.cat) {
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
                switch (expression.cat) {
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
                switch (expression.cat) {
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
                switch (expression.cat){
                    case AND -> {
                        return ConstInt.getI1(c1.getVal()&c2.getVal());
                    }
                    case OR -> {
                        return ConstInt.getI1(c1.getVal()|c2.getVal());
                    }
                }
            }
            case ZEXT, FPTOSI, SITOFP -> {
                switch (expression.cat){
                    case ZEXT -> {
                        var c1 = (ConstInt) expression.getOperandAt(0);
                        return ConstInt.getI32(c1.getVal());
                    }
                    case FPTOSI -> {
                        var c1 = (ConstFloat) expression.getOperandAt(0);
                        return ConstInt.getI32((int)c1.getVal());
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
        throw new RuntimeException("Unable to derive expression of type "+expression.cat);
    }

    static void deriveConstantExpression(Function function) {
        Queue<Instruction> queue = new ArrayDeque<>();
        for (BasicBlock basicBlock : function) {
            for (Instruction instruction : basicBlock) {
                if (canDeriveExpression(instruction)) {
                    queue.add(instruction);
                }
            }
        }
        while (!queue.isEmpty()) {
            Instruction expression = queue.remove();

            // Such PHI should be deleted instead of derive
            if(expression instanceof PhiInst phiInst && !phiInst.hasEntry()) continue;

            if (expression.getType().isVoidType()) {
                // Br, Load, Store, etc.
                if (expression instanceof TerminatorInst.Br br) {
                    optimizeBr(br,queue);
                }
            } else {
                Constant constant = calculateExpressionValue(expression);
                @SuppressWarnings("unchecked")
                var uses = (List<Use>) expression.getUses().clone();
                uses.forEach(use -> {
                    use.setUsee(constant);
                    if(use.getUser() instanceof Instruction user){
                        if(canDeriveExpression(user)){
                            queue.add(user);
                        }
                    }
                });
                expression.removeSelf();
            }
        }
    }

    /**
     * Optimize a Br instruction. <br>
     *
     * @param br The instruction to be optimized.
     */
    private static void optimizeBr(TerminatorInst.Br br, Queue<Instruction> deriveQueue) {
        if(!br.isCondJmp()) return;
        var cond_ = br.getOperandAt(0);
        if(!(cond_ instanceof ConstInt cond)) return;
        var bTrue = (BasicBlock) br.getOperandAt(1);
        var bFalse = (BasicBlock) br.getOperandAt(2);
        br.removeOperandAt(0);
        br.removeOperandAt(1);
        br.removeOperandAt(2);
        if(cond.getVal()==1){
            br.addOperandAt(bTrue,0);
            removeEntry(bFalse,br.getBB(),deriveQueue);
        }else{
            br.addOperandAt(bFalse,0);
            removeEntry(bTrue,br.getBB(),deriveQueue);
        }
    }

    // This function should not be here, but I don't know where to put it.
    /**
     * Remove one entry of a block.
     * @param basicBlock Basic block one of whose entry is removed.
     * @param entry The entry to be removed.
     */
    static void removeEntry(BasicBlock basicBlock, BasicBlock entry, Queue<Instruction> deriveQueue){
        for (Instruction instruction : basicBlock.instructions) {
            if(instruction instanceof PhiInst phiInst){
                phiInst.removeMapping(entry);
                if(phiInst.isConstant()){
                    deriveQueue.add(phiInst);
                }
            }else{
                break; // PHI must be in the front of a bb
            }
        }
        if(!isBlockIsolated(basicBlock)) return;
        basicBlock.removeSelf(); // Firstly remove self, otherwise isBlockIsolated may get a wrong result
        var followingBBs = RelationAnalysis.getFollowingBB(basicBlock);
        for (BasicBlock followingBlock : followingBBs) {
            removeEntry(followingBlock,basicBlock,deriveQueue);
        }
    }

    // This function shouldn't be here, too
    static boolean isBlockIsolated(BasicBlock basicBlock){
        for (BasicBlock otherBlock : basicBlock.getFunc()) {
            for (BasicBlock followingBlock : RelationAnalysis.getFollowingBB(otherBlock)) {
                if(basicBlock==followingBlock) return false;
            }
        }
        return true;
    }

}
