package ir.values.instructions;

import ir.Value;
import ir.Type;
import ir.types.FunctionType;
import ir.values.Function;
import ir.values.Instruction;

import java.util.ArrayList;

/**
 * A Terminator instruction is used to terminate a Basic Block.
 */
public class TerminatorInst {

    /**
     * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/Instructions.h#L1475>
     *     LLVM IR Source: CallInst</a>
     * @see <a href="https://llvm.org/docs/LangRef.html#call-instruction">
     *     LLVM LangRef: Call Instruction</a>
     */
    public static class Call extends Instruction {
        //<editor-fold desc="Constructors">
        /**
         * @param func Function Value carrying information about return type and FORMAL arguments.
         * @param args The ACTUAL arguments to be referenced by the Call.
         */
        public Call(Function func, ArrayList<Value> args) {
            // Operands of Call is the Function invoked and all argument Values passed
            // thus numOperands = 1 + args.size().
            super(((FunctionType)func.type).getRetType(), InstCategory.CALL, args.size()+1);

            // Call instruction will yield a result if the function has non-void return type.
            this.hasResult = !this.type.isVoidType();

            // The function Value is the 1st operand of the Call instruction.
            this.addOperandAt(func, 0);
            // All arguments as operands.
            for (int i = 0; i < args.size(); i++) {
                this.addOperandAt(args.get(i), i+1);
            }
        }
        //</editor-fold>

        //<editor-fold desc="Methods">
        @Override
        public String toString() {
            // e.g. %res = call i32 @func(i32 %arg) ; with return value
            // e.g. call void @func(i32 %arg)            ; without return value
            StringBuilder strBuilder = new StringBuilder();

            // "%res = " if the function call yields a result.
            if (this.hasResult) {
                strBuilder.append(this.name).append(" = ");
            }
            // "call i32 " or "call void"
            // + "@func(i32 %arg)"
            strBuilder.append("call ").append(this.type).append(" ")
                    .append("@").append(this.getOperandAt(0).name)
                    .append("(");
            for(int i = 1; i < this.getNumOperands(); i++) {
                Value opd = this.getOperandAt(i);
                strBuilder.append(opd.type)
                        .append(" ")
                        .append(opd.name);
                if (i != this.getNumOperands() - 1) { // The last argument need no comma following it.
                    strBuilder.append(", ");
                }
            }
            strBuilder.append(")");

            return strBuilder.toString();
        }
        //</editor-fold>
    }

    /**
     * Return Terminator corresponding to return statement.
     * <br>
     * Type for Ret is the return type (which maybe VoidType,
     * IntegerType and FloatType). It's noteworthy that no
     * pointer can be returned in SysY, thus Type shouldn't
     * be a PointerType instance.
     * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/Instructions.h#L2950">
     *     LLVM IR Source: ReturnInst</a>
     * @see <a href="https://llvm.org/docs/LangRef.html#ret-instruction">
     *     LLVM LangRef: Return Instruction</a>
     */
    public static class Ret extends Instruction {

        //<editor-fold desc="Constructors">
        /**
         * Construct an Ret terminator returning void.
         */
        public Ret() {
            super(Type.VoidType.getType(), InstCategory.RET, 0);
            this.hasResult = false;
        }

        /**
         * Construct a Ret terminator returning a Value.
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
