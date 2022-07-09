package ir.values.instructions;

import ir.Type;
import ir.Value;
import ir.types.FloatType;
import ir.types.IntegerType;
import ir.values.BasicBlock;
import ir.values.Instruction;

/**
 * This class contains nested classes representing instructions
 * for conversion between Values in different IR Types.
 * <br>
 * Type for a CastInst is the destination Type of the casting.
 */
public class CastInst {

    /**
     * Represents Zero Extension of integer type.
     * In our case, there is only the case of extending i1 to i32,
     * thus the destination type will only be i32.
     * <br>
     * Type for ZExt is its destination type (i32).
     * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/Instructions.h#L4758">
     *     LLVM IR Source: ZExtInst</a>
     * @see <a href="https://llvm.org/docs/LangRef.html#zext-to-instruction">
     *     LLVM LangRef: ZExt Instruction</a>
     */
    public static class ZExt extends Instruction {

        /**
         * Construct a ZExt instruction.
         * @param opd The operand Value in i1 to be extended.
         * @param bb  The parent BasicBlock to hold the ZExt.
         */
        public ZExt(Value opd, BasicBlock bb) {
            super(IntegerType.getI32(), InstCategory.ZEXT, bb);
            this.addOperandAt(opd, 0);
        }

        @Override
        public String toString() {
            return this.getName()
                    + " = "
                    + "zext i1 "
                    + this.getOperandAt(0).getName()
                    + " to i32";
        }
    }

    /**
     * Convert a FloatType Value into IntegerType.
     * @see <a href="https://llvm.org/docs/LangRef.html#fptosi-to-instruction">
     *     LLVM LangRef: ‘fptosi .. to’ Instruction</a>
     */
    public static class Fptosi extends Instruction {

        /**
         * Construct a fptosi instruction.
         * @param opd The operand Value to be casted.
         * @param destType The target IntegerType (maybe i32 or i1)
         * @param bb The parent BasicBlock holding the inst to be created.
         */
        public Fptosi(Value opd, IntegerType destType, BasicBlock bb) {
            super(destType, InstCategory.FPTOSI, bb);
            this.addOperandAt(opd, 0);
        }

        @Override
        public String toString() {
            // e.g. "%4 = fptosi float %3 to i32"
            Value opd = this.getOperandAt(0);
            return this.getName() + " = fptosi "// "%4 = fptosi "
                    + opd // "float %3"
                    + " to " + this.getType(); // " to i32"
        }
    }


    /**
     * Convert a IntegerType Value into FloatType.
     * @see <a href="https://llvm.org/docs/LangRef.html#sitofp-to-instruction">
     *     LLVM LangRef: ‘sitofp .. to’ Instruction</a>
     */
    public static class Sitofp extends Instruction {

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
            return this.getName() + " = sitofp "// "%6 = sitofp "
                    + opd.getType() + " " + opd.getName() // "i32 %5"
                    + " to " + this.getType(); // " to float"
        }
    }
}
