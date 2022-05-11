package ir.values.instructions;

import ir.Value;
import ir.Type;
import ir.values.Instruction;

/**
 * A Terminator instruction is used to terminate a Basic Block.
 */
public class TerminatorInst {

    /**
     * Return Terminator corresponding to return statement.
     * <br>
     * Type for Ret is the return type (which maybe VoidType,
     * IntegerType and FloatType).
     * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/Instructions.h#L2950">
     *     LLVM IR Reference</a>
     */
    public static class Ret extends Instruction {

        //<editor-fold desc="Constructors">
        /**
         * Construct an Ret terminator returning void.
         */
        public Ret() {
            super(Type.VoidType.getType(), InstCategory.RET, 0);
//            needName = false;
        }

        /**
         * Construct an Ret terminator returning a Value.
         * @param val
         */
        public Ret(Value val) {
            super(Type.VoidType.getType(), InstCategory.RET, 1);
            this.addOperandAt(val, 0);
//            needName = false;
        }
        //</editor-fold>


        //<editor-fold desc="Methods">
        @Override
        public String toString() {
            StringBuilder strBuilder = new StringBuilder();
            strBuilder.append("ret ");
            if (this.getNumOperands() == 1) {
                strBuilder.append(getOperandAt(0).type)
                        .append(" ")
                        .append(getOperandAt(0).name);
            } else {
                strBuilder.append("void");
            }
            return strBuilder.toString();
        }
        //</editor-fold>
    }
}
