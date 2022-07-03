package ir.values.instructions;

import ir.Value;
import ir.types.FloatType;
import ir.types.IntegerType;
import ir.values.BasicBlock;
import ir.values.Instruction;

/**
 * This class contains nested classes representing instructions
 * for conversion between Values in different IR Types.
 */
public class CastInst {

    /**
     * Convert a FloatType Value into IntegerType.
     * @see <a href="https://llvm.org/docs/LangRef.html#fptosi-to-instruction">
     *     LLVM LangRef: ‘fptosi .. to’ Instruction</a>
     */
    public static class Fptosi extends Instruction {

        /**
         * Represents the target type (i32 or i1).
         */
        private final IntegerType targetType;

        /**
         * Construct a fptosi instruction.
         * @param opd The operand Value to be casted.
         * @param targetType The target IntegerType (maybe i32 or i1)
         * @param bb The parent BasicBlock holding the inst to be created.
         */
        public Fptosi(Value opd, IntegerType targetType, BasicBlock bb) {
            super(FloatType.getType(), InstCategory.FPTOSI, bb);
            this.addOperandAt(opd, 0);
            this.targetType = targetType;
        }

        @Override
        public String toString() {
            // e.g. "%4 = fptosi float %3 to i32"
            Value opd = this.getOperandAt(0);
            return this.getName() + " = fptosi "// "%4 = fptosi "
                    + opd // "float %3"
                    + " to " + this.targetType; // " to i32"
        }
    }


    /**
     * Convert a IntegerType Value into FloatType.
     * @see <a href="https://llvm.org/docs/LangRef.html#sitofp-to-instruction">
     *     LLVM LangRef: ‘sitofp .. to’ Instruction</a>
     */
    public static class Sitofp extends Instruction {

        // We only got one kind of FloatType thus it's a fixed field.
        private final FloatType targetType = FloatType.getType();

        /**
         * Construct a sitofp instruction.
         * @param opd The operand Value to be casted.
         * @param bb The parent BasicBlock holding the inst to be created.
         */
        public Sitofp(Value opd, BasicBlock bb) {
            super(FloatType.getType(), InstCategory.SITOFP, bb);
            this.addOperandAt(opd, 0);
        }

        @Override
        public String toString() {
            // e.g. "%6 = sitofp i32 %5 to float"
            Value opd = this.getOperandAt(0);
            return this.getName() + " = fptosi "// "%6 = sitofp "
                    + opd // "i32 %5"
                    + " to " + this.targetType; // " to float"
        }
    }
}
