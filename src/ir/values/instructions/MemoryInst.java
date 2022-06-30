package ir.values.instructions;

import ir.Type;
import ir.Value;
import ir.types.IntegerType;
import ir.types.PointerType;
import ir.values.BasicBlock;
import ir.values.Instruction;

/**
 * This class contains nested classes represent instructions that are memory-related,
 * including memory accessing and addressing instructions.
 * In the latest version of LLVM IR, these instructions inherit from Instruction (Store)
 * or Instruction.UnaryInstruction (Load, Alloca, ZExt).
 */
public class MemoryInst {

    /**
     * An instruction for writing to memory.
     * A Store has two arguments (operands): a value to store
     * and an address at which to store it, and does NOT yield
     * any result (yields void, need no name).
     * <br>
     * Thus, Type for Store is VoidType.
     * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/Instructions.h#L304">
     *     LLVM IR Source: StoreInst</a>
     * @see <a href="https://llvm.org/docs/LangRef.html#store-instruction">
     *     LLVM LangRef: Store Instruction</a>
     */
    public static class Store extends Instruction {

        //<editor-fold desc="Constructors">
        /**
         * @param val The Value to be stored (written) back to memory.
         * @param addr The address where the content to be written.
         */
        public Store(Value val, Value addr, BasicBlock bb) {
            super(Type.VoidType.getType(), InstCategory.STORE, bb);
            this.addOperandAt(val, 0);
            this.addOperandAt(addr, 1);
            this.hasResult = false;
        }
        //</editor-fold>


        //<editor-fold desc="Methods">
        @Override
        public String toString() {
            Value val = this.getOperandAt(0);
            Value addr = this.getOperandAt(1);
            // e.g. store i32 3, i32* %ptr
            return "store " // store
                    + val.getType() + " " + val.getName() + ", " // i32 3,
                    + addr.getType() + " " + addr.getName(); // i32* %ptr
        }
        //</editor-fold>
    }

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
     *     LLVM LangRef: Load Instruction</a>
     */
    public static class Load extends Instruction {

        //<editor-fold desc="Constructors">
        /**
         * @param loadedType  The type of the memory block loaded in.
         * @param addr Value specifying the memory address from which to load. (loadedType*)
         */
        public Load(Type loadedType, Value addr, BasicBlock bb) {
            super(loadedType, InstCategory.LOAD, bb);
            this.addOperandAt(addr, 0);
        }
        //</editor-fold>


        //<editor-fold desc="Methods">
        @Override
        public String toString() {
            Value op = this.getOperandAt(0);
            // e.g. %val = load i32, i32* %ptr
            return this.getName() + " = load " // %val = load
                    + this.getType() + ", " // i32,
                    + op.getType() + " " + op.getName(); // , i32* %ptr
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
     *     LLVM LangRef: Alloca Instrucstion</a>
     */
    public static class Alloca extends Instruction {
        //<editor-fold desc="Fields">
        /**
         * The type of memory space allocated.
         */
        private final Type allocatedType;
//        private boolean isInit = false;
        //</editor-fold>

        public Type getAllocatedType() {
            return allocatedType;
        }


        //<editor-fold desc="Constructors">

        /**
         * @param allocatedType The type of memory space allocated.
         */
        public Alloca(Type allocatedType, BasicBlock bb) {
            super(PointerType.getType(allocatedType), InstCategory.ALLOCA, bb);
            this.allocatedType = allocatedType;
        }
        //</editor-fold>

        //<editor-fold desc="Methods">
        @Override
        public String toString() {
            return this.getName() + " = alloca " + this.allocatedType;
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
     *     LLVM IR Source: ZExtInst</a>
     * @see <a href="https://llvm.org/docs/LangRef.html#zext-to-instruction">
     *     LLVM LangRef: ZExt Instruction</a>
     */
    public static class ZExt extends Instruction {
        //<editor-fold desc="Fields">
        private final static Type destType = IntegerType.getI32();
        //</editor-fold>


        //<editor-fold desc="Constructors">
        public ZExt(Value val, BasicBlock bb) {
            super(destType, InstCategory.ZEXT, bb);
            this.addOperandAt(val, 0);
        }
        //</editor-fold>


        //<editor-fold desc="Methods">
        @Override
        public String toString() {
            return this.getName()
                    + " = "
                    + "zext i1 "
                    + this.getOperandAt(0).getName()
                    + " to i32";
        }
        //</editor-fold>
    }

}
