package passes.ir.simplify;

import ir.Module;
import ir.Use;
import ir.Value;
import ir.types.IntegerType;
import ir.values.Function;
import ir.values.Instruction;
import ir.values.constants.ConstInt;
import ir.values.instructions.BinaryOpInst;
import passes.PassManager;
import passes.ir.IRPass;
import passes.ir.gcm.GlobalCodeMotion;
import passes.mc.registerAllocation.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * <p>Merge add instructions.</p>
 * <p>This pass is designed for the test set.</p>
 * <p>It tries to merge add instructions into multiply ones.</p>
 */
public class AddInstMerge implements IRPass {

    /**
     * An expression like a1*v1+a2*v2+...+an*vn.
     */
    static class Expression {
        Map<Value, Integer> terms = new HashMap<>();

        private Expression() {
        }

        private Expression(Value value) {
            terms.put(value, 1);
        }

        /**
         * Write this expression as instructions.
         * @return Instructions to calculate this expression and the final value of this expression.
         */
        public Pair<List<Instruction>,Value> getInstructions(){
            List<Instruction> instructionList = new ArrayList<>();
            AtomicInteger sumConstant = new AtomicInteger();
            Value finalValue = ConstInt.getI32(0);
            java.util.function.Function<Instruction,Instruction> appendInst = (instruction) -> {
                instructionList.add(instruction);
                return instruction;
            };
            for (Value value : terms.keySet()) {
                var time = terms.get(value);
                if(time==0) continue;
                if(value instanceof ConstInt){
                    sumConstant.addAndGet(((ConstInt) value).getVal() * time);
                }else{
                    if(time==1){
                        finalValue = appendInst.apply(new BinaryOpInst(IntegerType.getI32(), Instruction.InstCategory.ADD, finalValue, value));
                    }else if(time==-1){
                        finalValue = appendInst.apply(new BinaryOpInst(IntegerType.getI32(), Instruction.InstCategory.SUB, finalValue, value));
                    }else{
                        var mulInst = appendInst.apply(new BinaryOpInst(IntegerType.getI32(), Instruction.InstCategory.MUL, value, ConstInt.getI32(time)));
                        finalValue = appendInst.apply(new BinaryOpInst(IntegerType.getI32(), Instruction.InstCategory.ADD, finalValue, mulInst));
                    }
                }
            }
            if(sumConstant.get()!=0){
                finalValue = appendInst.apply(new BinaryOpInst(IntegerType.getI32(), Instruction.InstCategory.ADD, finalValue, ConstInt.getI32(sumConstant.get())));
            }
            return new Pair<>(instructionList, finalValue);
        }

        /**
         * Get expression for the given value(instruction).
         * @param value Given value.
         * @param expressionMap A map to accelerate.
         * @return The expression for the value.
         */
        public static Expression fromValue(Value value, Map<Instruction, Expression> expressionMap) {
            if (value instanceof Instruction) {
                var instruction = (Instruction) value;
                if (isAddOrSub(instruction)) {
                    if (expressionMap.containsKey(instruction)) {
                        return expressionMap.get(instruction);
                    } else {
                        var result = combine(
                                Expression.fromValue(instruction.getOperandAt(0), expressionMap),
                                Expression.fromValue(instruction.getOperandAt(1), expressionMap),
                                isSub(instruction)
                        );
                        expressionMap.put(instruction, result);
                        return result;
                    }
                } else {
                    return new Expression(instruction);
                }
            } else {
                return new Expression(value);
            }
        }

        /**
         * Add two expressions.
         */
        private static Expression combine(Expression lhs, Expression rhs, boolean isSub) {
            var expression = new Expression();
            BiConsumer<Value, Integer> add = (value, time) -> {
                if (expression.terms.containsKey(value)) {
                    expression.terms.put(value, expression.terms.get(value) + time);
                } else {
                    expression.terms.put(value, time);
                }
            };
            BiConsumer<Value, Integer> sub = (value, time) -> {
                if (expression.terms.containsKey(value)) {
                    expression.terms.put(value, expression.terms.get(value) - time);
                } else {
                    expression.terms.put(value, -time);
                }
            };
            lhs.terms.forEach(add);
            rhs.terms.forEach(isSub?sub:add);
            return expression;
        }

        private static boolean isAdd(Instruction instruction) {
            return instruction.getTag() == Instruction.InstCategory.ADD;
        }

        private static boolean isSub(Instruction instruction) {
            return instruction.getTag() == Instruction.InstCategory.SUB;
        }

        private static boolean isAddOrSub(Instruction instruction) {
            return isAdd(instruction) || isSub(instruction);
        }

    }

    @Override
    public void runOnModule(Module module) {
        module.functions.forEach(AddInstMerge::optimize);
        PassManager.getInstance().run(GlobalCodeMotion.class, module);
    }

    private static void optimize(Function function) {
        Map<Instruction, Expression> expressionMap = new HashMap<>();
        function.forEach(basicBlock -> basicBlock.forEach(instruction -> Expression.fromValue(instruction, expressionMap)));
        function.forEach(basicBlock -> basicBlock.forEach(instruction -> {
            if(!expressionMap.containsKey(instruction)) return;
            boolean needToOptimize = false;
            for (Use use : instruction.getUses()) {
                var user = (Instruction) use.getUser();
                if(expressionMap.containsKey(user)) continue;
                needToOptimize = true;
            }
            if(!needToOptimize) return;
            var expression = expressionMap.get(instruction);
            var pair = expression.getInstructions();
            var instructionList = pair.getA();
            var finalValue = pair.getB();
            instructionList.forEach(newInstruction -> newInstruction.insertBefore(instruction));
            instruction.replaceSelfTo(finalValue);
        }));
    }

}
