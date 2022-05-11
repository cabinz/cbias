package ir.values.instructions;

import ir.Type;
import ir.Value;
import ir.types.IntegerType;
import ir.types.PointerType;
import ir.values.Instruction;

/**
 * This class contains nested classes represent instructions that are memory-related,
 * including memory accessing and addressing instructions.
 * In the latest version of LLVM IR, these instructions inherit from Instruction (Store)
 * or Instruction.UnaryInstruction (Load, Alloca, ZExt).
 */
public class MemoryInst {

    /**
     * Represents a ‘load’ instruction used to read from memory,
     * which yields the result of loaded memory block.
     * The argument (operand) to the load instruction specifies
     * the memory address from which to load.
     * <br>
     * Type for Load is the type of the memory block loaded in.
     * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/Instructions.h#L175">
     *     LLVM IR Source: LoadInst</a>
     * @see <a href="https://llvm.org/docs/LangRef.html#load-instruction">
     *     LLVM Lang Ref: Load Instruction</a>
     */
    public static class Load extends Instruction {

        //<editor-fold desc="Constructors">
        /**
         * @param loadedType  The type of the memory block loaded in.
         * @param addr Value specifying the memory address from which to load. (loadedType*)
         */
        public Load(Type loadedType, Value addr) {
            super(loadedType, InstCategory.LOAD, 1);
            this.addOperandAt(addr, 0);
        }
        //</editor-fold>


        //<editor-fold desc="Methods">
        @Override
        public String toString() {
            Value op = this.getOperandAt(0);

            // e.g. %val = load i32, i32* %ptr
            return this.name + " = load " // %val = load
                    + this.type + ", " // i32,
                    + op.type + " " + op.name; // , i32* %ptr
        }
        //</editor-fold>
    }

    /**
     * Alloca is an instruction to allocate memory on the stack,
     * yielding an address (pointer) where the memory allocated
     * lands as result.
     * <br>
     * Type for Alloca is PointerType.
     * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/Instructions.h#L62">
     *     LLVM IR Source: AllocaInst</a>
     * @see <a href="https://llvm.org/docs/LangRef.html#alloca-instruction">
     *     LLVM Language Reference: Alloca Instrucstion</a>
     */
    public static class Alloca extends Instruction {
        //<editor-fold desc="Fields">
        public final Type allocatedType;
//        private boolean isInit = false;
        //</editor-fold>


        //<editor-fold desc="Constructors">
        public Alloca(Type allocatedType) {
            super(PointerType.getType(allocatedType), InstCategory.ALLOCA, 0);
            this.allocatedType = allocatedType;
        }
        //</editor-fold>

        //<editor-fold desc="Methods">
        @Override
        public String toString() {
            return this.name + " = alloca " + allocatedType.toString();
        }
        //</editor-fold>
    }

    /**
     * Represents Zero Extension of integer type.
     * In our case, there is only the case of extending i1 to i32,
     * thus the destination type will only be i32.
     * <br>
     * Type for ZExt is its destination type (i32).
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
