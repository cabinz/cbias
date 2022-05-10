package ir.values.instructions;

import ir.Type;
import ir.Value;
import ir.types.IntegerType;
import ir.values.Instruction;

/**
 * This class contains nested classes represent instructions that are memory-related.
 * In LLVM IR, these instructions inherit from Instruction (Store)
 * or Instruction.UnaryInstruction (Load, Alloca, ZExt).
 */
public class MemoryInst {

    /**
     * Represents Zero Extension of integer type.
     * In our case, there is only the case of extending i1 to i32,
     * thus the destination type will only be i32.
     * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/Instructions.h#L4758">
     *     LLVM IR Source</a>
     * @see <a href="https://llvm.org/docs/LangRef.html#zext-to-instruction">
     *     LLVM IR Docs</a>
     */
    public static class ZExt extends Instruction {
        //<editor-fold desc="Fields">
        private final static Type destType = IntegerType.getI32();
        //</editor-fold>


        //<editor-fold desc="Constructors">
        public ZExt(Value val) {
            super(destType, InstCategory.ZEXT, 1);
            this.addOperandAt(val, 0);
        }
        //</editor-fold>


        //<editor-fold desc="Methods">
        @Override
        public String toString() {
            return this.name
                    + " = "
                    + "zext i1 "
                    + this.getOperandAt(0).name
                    + " to i32";
        }
        //</editor-fold>
    }

}
