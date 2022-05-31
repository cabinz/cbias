package ir.values.instructions;

import ir.Value;
import ir.Type;
import ir.types.FunctionType;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.Instruction;

import java.util.ArrayList;

/**
 * A Terminator instruction is used to terminate a Basic Block.
 */
public class TerminatorInst {

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
         * Construct a Ret terminator returning void.
         */
        public Ret(BasicBlock bb) {
            super(Type.VoidType.getType(), InstCategory.RET, bb);
            this.hasResult = false;
        }

        /**
         * Construct a Ret terminator returning a Value.
         * @param val The return value.
         */
        public Ret(Value val, BasicBlock bb) {
            this(bb);
            this.addOperandAt(val, 0);
        }
        //</editor-fold>


        //<editor-fold desc="Methods">
        @Override
        public String toString() {
            StringBuilder strBuilder = new StringBuilder();
            strBuilder.append("ret ");
            if (this.getNumOperands() == 1) {
                strBuilder.append(getOperandAt(0).getType())
                        .append(" ")
                        .append(getOperandAt(0).getName());
            } else {
                strBuilder.append("void");
            }
            return strBuilder.toString();
        }
        //</editor-fold>
    }


    /**
     * A Br terminator causes control flow to transfer to a different basic block in the current function.
     * <br>
     * Br has two forms:
     * <ul>
     *     <li>Conditional Branch: has 3 operands (1 conditional variable i1, 2 destination Basic Blocks)</li>
     *     <li>Unconditional Branch: has 1 operands (of the destination label)</li>
     * </ul>
     * Type for Br is LabelType (referenced by a destination BasicBlock).
     * @see <a href="https://llvm.org/docs/LangRef.html#br-instruction">
     *     LLVM LangRef: 'br' Instruction</a>
     */
    public static class Br extends Instruction {

        //<editor-fold desc="Constructors">
        /**
         * Constructor for a conditional branching instruction.
         * @param cond The condition.
         * @param trueBlk The basic block to jump to when condition is true.
         * @param falseBlk The basic block to jump to when condition is false.
         */
        public Br(Value cond, BasicBlock trueBlk, BasicBlock falseBlk, BasicBlock bb) {
            // todo: Encapsulate numOperands to be a automatically increased counter.
            super(Type.LabelType.getType(), InstCategory.BR, bb);
            // todo: Type of Br should be VoidType or LabelType?
            this.hasResult = false;
            this.addOperandAt(cond, 0);
            this.addOperandAt(trueBlk, 1);
            this.addOperandAt(falseBlk, 2);
        }

        /**
         * Constructor for an unconditional branching instruction.
         * @param blk The basic block to jump to.
         */
        public Br(BasicBlock blk, BasicBlock bb) {
            super(Type.LabelType.getType(), InstCategory.BR, bb);
            this.hasResult = false;
            this.addOperandAt(blk, 0);
        }
        //</editor-fold>


        //<editor-fold desc="Methods">
        @Override
        public String toString() {
            StringBuilder strBuilder = new StringBuilder();
            strBuilder.append("br ");
            // Print operands.
            for(int i = 0; i < this.getNumOperands(); i++) {
                Value opr = getOperandAt(i);
                strBuilder.append(opr.getType())
                        .append(opr.getType().isLabelType() ? " %" : " ")
                        .append(opr.getName());
                // The last operand need no comma following it.
                if (i != this.getNumOperands() - 1) {
                    strBuilder.append(", ");
                }
            }

            return strBuilder.toString();
        }
        //</editor-fold>
    }
}
