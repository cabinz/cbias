package passes.ir.constant_derivation;

import ir.Module;
import ir.Use;
import ir.Value;
import ir.types.VoidType;
import ir.values.*;
import ir.values.constants.ConstArray;
import ir.values.constants.ConstFloat;
import ir.values.constants.ConstInt;
import ir.values.instructions.*;
import passes.PassManager;
import passes.ir.DummyValue;
import passes.ir.IRPass;
import passes.ir.analysis.RelationAnalysis;
import passes.ir.analysis.RelationUtil;
import passes.ir.dce.UnreachableCodeElim;

import java.util.*;

/**
 * This optimization includes the following passes: <br />
 * 1) Derive global constant.
 * 2) Derive const instruction.
 * 3) Derive const branch condition.
 */
public class ConstantDerivation implements IRPass {

    static boolean hasNonEntryBlock = false;

    @Override
    public synchronized void runOnModule(Module module) {
        while (true) {
            optimize(module);
            if (hasNonEntryBlock) {
                PassManager.getInstance().run(UnreachableCodeElim.class, module);
                hasNonEntryBlock = false;
            } else {
                break;
            }
        }
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
     * Judge whether the value is a constant.
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
     * Judge whether an expression can be derived.
     *
     * @param expression The expression to be judged.
     */
    static boolean canDeriveExpression(Instruction expression) {
        return calculateExpressionValue(expression)!=expression;
    }

    private static boolean canDeriveGep(GetElemPtrInst gep) {
        for (int i = 1; i < gep.getNumOperands(); i++) {
            if (!(gep.getOperandAt(i) instanceof ConstInt)) return false;
        }
        while (gep.getOperandAt(0) instanceof GetElemPtrInst) {
            gep = (GetElemPtrInst) gep.getOperandAt(0);
            for (int i = 1; i < gep.getNumOperands(); i++) {
                if (!(gep.getOperandAt(i) instanceof ConstInt)) return false;
            }
        }
        var target = gep.getOperandAt(0);
        if (target instanceof GlobalVariable) {
            var gv = (GlobalVariable) target;
            if (gv.isConstant()) return true;
        }
        return false;
    }

    /**
     * Running through gep instruction to get the value it points to.
     * @param array Array for the gep instruction. (Not using the one in gep because it may be another gep instruction.)
     * @param gep Gep instruction. (Only use its values.)
     * @param finalOffset Extra offset for the last dimension. (This parameter comes from the strange definition of gep instruction.)
     * @return The value gep points to.
     */
    static Value runGep(ConstArray array, GetElemPtrInst gep, int finalOffset) {
        if (array == null) {
            var retType = gep.getType().getPointeeType();
            if (retType.isArrayType()) return null;
            if (retType.isIntegerType()) return ConstInt.getI32(0);
            if (retType.isFloatType()) return ConstFloat.get(0);
            throw new RuntimeException("Unknown return type " + gep.getType());
        }
        Value result = array;
        for (int i = 2; i < gep.getNumOperands(); i++) { // I don't know why it starts from 2
            var index = ((ConstInt) gep.getOperandAt(i)).getVal();
            if(i==gep.getNumOperands()-1){
                index += finalOffset;
            }
            result = ((ConstArray) result).getOperandAt(index);
        }
        return result;
    }

    /**
     * Get the value of a gep instruction recursively.
     * @param gep Gep instruction.
     * @param offset Extra offset for the last dimension. (This parameter comes from the strange definition of gep instruction.)
     * @return The value of gep instruction.
     */
    static Value deriveGepValue(GetElemPtrInst gep, int offset) {
        var target = gep.getOperandAt(0);
        if (target instanceof GetElemPtrInst) {
            if(gep.getNumOperands()==2){
                return deriveGepValue((GetElemPtrInst) target, ((ConstInt)gep.getOperandAt(1)).getVal());
            }else{
                return runGep((ConstArray) deriveGepValue((GetElemPtrInst) target, ((ConstInt)gep.getOperandAt(1)).getVal()), gep, offset);
            }
        }
        if (target instanceof GlobalVariable) {
            var gv = (GlobalVariable) target;
            if (gv.isConstant()) {
                return runGep((ConstArray) gv.getInitVal(), gep, offset);
            }
        }
        throw new RuntimeException("Cannot derive gep with operand[0]=" + target);
    }

    /**
     * Trying to simplify the expression given.
     * @param expression The expression to be optimized.
     * @return A simplified expression or the original expression(If cannot optimize).
     */
    static Value calculateExpressionValue(Instruction expression) {
        // Refuse to derive value outside bb, because they may be invalid instructions.
        // Actually, valid check should be inside switch below while necessity to derive should be checked in deriveConstantExpression
        if(expression.getBB()==null) return expression;
        switch (expression.getTag()) {
            case ADD, SUB, MUL, DIV -> {
                var c1 = expression.getOperandAt(0);
                var c2 = expression.getOperandAt(1);
                switch (expression.getTag()) {
                    case ADD -> {
                        // 1+2 = 3
                        if (c1 instanceof ConstInt && c2 instanceof ConstInt) {
                            return ConstInt.getI32(((ConstInt) c1).getVal() + ((ConstInt) c2).getVal());
                        }
                        // 0+x = x
                        if (c1 instanceof ConstInt && ((ConstInt) c1).getVal() == 0) {
                            return c2;
                        }
                        // x+0 = x
                        if (c2 instanceof ConstInt && ((ConstInt) c2).getVal() == 0) {
                            return c1;
                        }
                        return expression;
                    }
                    case SUB -> {
                        // 3-2 = 1
                        if (c1 instanceof ConstInt && c2 instanceof ConstInt) {
                            return ConstInt.getI32(((ConstInt) c1).getVal() - ((ConstInt) c2).getVal());
                        }
                        // x-0 = x
                        if (c2 instanceof ConstInt && ((ConstInt) c2).getVal() == 0) {
                            return c1;
                        }
                        // x-x = 0
                        if(Objects.equals(c1,c2)){
                            return ConstInt.getI32(0);
                        }
                        return expression;
                    }
                    case MUL -> {
                        // 2*3 = 6
                        if (c1 instanceof ConstInt && c2 instanceof ConstInt) {
                            return ConstInt.getI32(((ConstInt) c1).getVal() * ((ConstInt) c2).getVal());
                        }
                        // 1*x = x
                        if (c1 instanceof ConstInt && ((ConstInt) c1).getVal() == 1) {
                            return c2;
                        }
                        // x*1 = x
                        if (c2 instanceof ConstInt && ((ConstInt) c2).getVal() == 1) {
                            return c1;
                        }
                        // Todo: consider x*(-1) here?
                        // 0*x = 0, x*0 = 0
                        if ((c1 instanceof ConstInt && ((ConstInt) c1).getVal() == 0) || (c2 instanceof ConstInt && ((ConstInt) c2).getVal() == 0)) {
                            return ConstInt.getI32(0);
                        }
                        return expression;
                    }
                    case DIV -> {
                        // 6/2 = 3
                        if (c1 instanceof ConstInt && c2 instanceof ConstInt) {
                            return ConstInt.getI32(((ConstInt) c1).getVal() / ((ConstInt) c2).getVal());
                        }
                        // x/1 = x
                        if (c2 instanceof ConstInt && ((ConstInt) c2).getVal() == 1) {
                            return c1;
                        }
                        // x/x = 1
                        if(Objects.equals(c1,c2)){
                            return ConstInt.getI32(1);
                        }
                        // Todo: consider x/(-1) here?
                        return expression;
                    }
                }
            }
            case FADD, FSUB, FMUL, FDIV -> {
                var c1 = expression.getOperandAt(0);
                var c2 = expression.getOperandAt(1);
                switch (expression.getTag()) {
                    case FADD -> {
                        // 1+2 = 3
                        if (c1 instanceof ConstFloat && c2 instanceof ConstFloat) {
                            return ConstFloat.get(((ConstFloat) c1).getVal() + ((ConstFloat) c2).getVal());
                        }
                        // 0+x = x
                        if (c1 instanceof ConstFloat && ((ConstFloat) c1).getVal() == 0) {
                            return c2;
                        }
                        // x+0 = x
                        if (c2 instanceof ConstFloat && ((ConstFloat) c2).getVal() == 0) {
                            return c1;
                        }
                        return expression;
                    }
                    case FSUB -> {
                        // 3-2 = 1
                        if (c1 instanceof ConstFloat && c2 instanceof ConstFloat) {
                            return ConstFloat.get(((ConstFloat) c1).getVal() - ((ConstFloat) c2).getVal());
                        }
                        // x-0 = x
                        if (c2 instanceof ConstFloat && ((ConstFloat) c2).getVal() == 0) {
                            return c1;
                        }
                        // x-x = 0
                        if(Objects.equals(c1,c2)){
                            return ConstFloat.get(0);
                        }
                        return expression;
                    }
                    case FMUL -> {
                        // 2*3 = 6
                        if (c1 instanceof ConstFloat && c2 instanceof ConstFloat) {
                            return ConstFloat.get(((ConstFloat) c1).getVal() * ((ConstFloat) c2).getVal());
                        }
                        // 1*x = x
                        if (c1 instanceof ConstFloat && ((ConstFloat) c1).getVal() == 1) {
                            return c2;
                        }
                        // x*1 = x
                        if (c2 instanceof ConstFloat && ((ConstFloat) c2).getVal() == 1) {
                            return c1;
                        }
                        // Todo: consider x*(-1) here?
                        // 0*x = 0, x*0 = 0
                        if ((c1 instanceof ConstFloat && ((ConstFloat) c1).getVal() == 0) || (c2 instanceof ConstFloat && ((ConstFloat) c2).getVal() == 0)) {
                            return ConstFloat.get(0);
                        }
                        return expression;
                    }
                    case FDIV -> {
                        // 6/2 = 3
                        if (c1 instanceof ConstFloat && c2 instanceof ConstFloat) {
                            return ConstFloat.get(((ConstFloat) c1).getVal() / ((ConstFloat) c2).getVal());
                        }
                        // x/1 = x
                        if (c2 instanceof ConstFloat && ((ConstFloat) c2).getVal() == 1) {
                            return c1;
                        }
                        // x/x = 1
                        if(Objects.equals(c1,c2)){
                            return ConstFloat.get(1);
                        }
                        // Todo: consider x/(-1) here?
                        return expression;
                    }
                }
            }
            case FNEG -> {
                var c1 = expression.getOperandAt(0);
                if (c1 instanceof ConstFloat) {
                    return ConstFloat.get(-((ConstFloat) c1).getVal());
                }
                return expression;
            }
            case LT, GT, EQ, NE, LE, GE -> {
                var rc1 = expression.getOperandAt(0);
                var rc2 = expression.getOperandAt(1);
                if (rc1 instanceof ConstInt && rc2 instanceof ConstInt) {
                    var c1 = (ConstInt) rc1;
                    var c2 = (ConstInt) rc2;
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
                if(rc1==rc2){
                    switch (expression.getTag()) {
                        case LT, NE, GT -> {
                            return ConstInt.getI1(0);
                        }
                        case EQ, LE, GE -> {
                            return ConstInt.getI1(1);
                        }
                    }
                }
                return expression;
            }
            case FLT, FGT, FEQ, FNE, FLE, FGE -> {
                var rc1 = expression.getOperandAt(0);
                var rc2 = expression.getOperandAt(1);
                if (rc1 instanceof ConstFloat && rc2 instanceof ConstFloat) {
                    var c1 = (ConstFloat) rc1;
                    var c2 = (ConstFloat) rc2;
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
                if(rc1==rc2){
                    switch (expression.getTag()) {
                        case FLT, FNE, FGT -> {
                            return ConstInt.getI1(0);
                        }
                        case FEQ, FLE, FGE -> {
                            return ConstInt.getI1(1);
                        }
                    }
                }
                return expression;
            }
            case AND, OR -> {
                var c1 = expression.getOperandAt(0);
                var c2 = expression.getOperandAt(0);
                switch (expression.getTag()) {
                    case AND -> {
                        if ((c1 instanceof ConstInt && ((ConstInt) c1).getVal() == 0) || (c2 instanceof ConstInt && ((ConstInt) c2).getVal() == 0)) {
                            return ConstInt.getI1(0);
                        }
                        if ((c1 instanceof ConstInt && ((ConstInt) c1).getVal() == 1) && (c2 instanceof ConstInt && ((ConstInt) c2).getVal() == 1)) {
                            return ConstInt.getI1(1);
                        }
                        if(c1==c2) return c1;
                        return expression;
                    }
                    case OR -> {
                        if ((c1 instanceof ConstInt && ((ConstInt) c1).getVal() == 1) || (c2 instanceof ConstInt && ((ConstInt) c2).getVal() == 1)) {
                            return ConstInt.getI1(1);
                        }
                        if ((c1 instanceof ConstInt && ((ConstInt) c1).getVal() == 0) && (c2 instanceof ConstInt && ((ConstInt) c2).getVal() == 0)) {
                            return ConstInt.getI1(0);
                        }
                        if(c1==c2) return c1;
                        return expression;
                    }
                }
            }
            case ZEXT, FPTOSI, SITOFP, PTRCAST -> {
                switch (expression.getTag()) {
                    case ZEXT -> {
                        var rc1 = expression.getOperandAt(0);
                        if (rc1 instanceof ConstInt) {
                            var c1 = (ConstInt) rc1;
                            return ConstInt.getI32(c1.getVal());
                        }
                        return expression;
                    }
                    case FPTOSI -> {
                        var rc1 = expression.getOperandAt(0);
                        if (rc1 instanceof ConstFloat) {
                            var c1 = (ConstFloat) rc1;
                            return ConstInt.getI32((int) c1.getVal());
                        }
                        return expression;
                    }
                    case SITOFP -> {
                        var rc1 = expression.getOperandAt(0);
                        if (rc1 instanceof ConstInt) {
                            var c1 = (ConstInt) expression.getOperandAt(0);
                            return ConstFloat.get((float) c1.getVal());
                        }
                        return expression;
                    }
                    case PTRCAST -> {
                        return expression;
                    }
                }
            }
            case PHI -> {
                var phiInst = (PhiInst) expression;
                if(phiInst.canDerive()){
                    return ((PhiInst) expression).deriveConstant();
                }
                return expression;
            }
            case LOAD,STORE,ALLOCA,CALL -> {
                switch (expression.getTag()){
                    case LOAD -> {
                        var retType = expression.getType();
                        if((expression.getOperandAt(0) instanceof GetElemPtrInst)&&(retType.isIntegerType() || retType.isFloatType())){
                            var gep = (GetElemPtrInst) expression.getOperandAt(0);
                            if(canDeriveGep(gep)){
                                return deriveGepValue(gep,0);
                            }
                        }
                        return expression;
                    }
                    default -> {
                        return expression;
                    }
                }
            }
            case BR, RET -> {
                switch (expression.getTag()){
                    case BR -> {
                        var brInst = (TerminatorInst.Br) expression;
                        var rc1 = expression.getOperandAt(0);
                        if(rc1 instanceof ConstInt){
                            return new DummyValue(VoidType.getType()); // Return something different to itself.
                        }
                        if(brInst.isCondJmp()&&Objects.equals(expression.getOperandAt(1),expression.getOperandAt(2))){
                            return new DummyValue(VoidType.getType()); // Return something different to itself.
                        }
                        return expression;
                    }
                    case RET -> {
                        return expression;
                    }
                }
            }
            case GEP -> {
                return expression;
            }
        }
        throw new RuntimeException("Unable to derive expression of type " + expression.getTag());
    }

    /**
     * Optimize a function.
     * @param function Function to be optimized.
     */
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

            if (!canDeriveExpression(expression)) continue;

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
                    if (canDeriveExpression(inst)) {
                        queue.add(inst);
                    }
                }
            }
        }
    }

    /**
     * Try to simplify a br instruction.
     * @param basicBlockMap Mapping for BasicBlock with extra information.
     * @param deriveQueue DeriveQueue in deriveConstantExpression
     * @param br Br instruction to be optimized.
     */
    private static void optimizeBr(Map<ir.values.BasicBlock, BasicBlock> basicBlockMap, Queue<Instruction> deriveQueue, TerminatorInst.Br br) {
        if (!br.isCondJmp()) return;
        var o0 = br.getOperandAt(0);
        if (o0 instanceof ConstInt){
            var cond = (ConstInt) o0;
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
            return;
        }
        if(Objects.equals(br.getOperandAt(1),br.getOperandAt(2))){
            var b = (ir.values.BasicBlock) br.getOperandAt(1);
            br.removeOperandAt(0);
            br.removeOperandAt(1);
            br.removeOperandAt(2);
            br.addOperandAt(0, b);
            // Do not need to remove entry since the same entry will only be logged once.
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
        RelationUtil.removeEntry(basicBlock.getRawBasicBlock(), entry.getRawBasicBlock());
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
        if (basicBlock.prevBlocks.size() == 0) {
            hasNonEntryBlock = true;
        }
    }

}
