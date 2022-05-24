package ir.values.instructions;

import ir.Type;
import ir.Value;
import ir.types.IntegerType;
import ir.values.BasicBlock;
import ir.values.Instruction;

/**
 * BinaryInst represents instructions with two operands,
 * e.g. binary arithmetic operations, comparison operations.
 * <br>
 * <ul>
 *     <li>Type for arithmetic instructions is the type of operation result.</li>
 *     <li>Type for comparison instructions is boolean (i1).</li>
 * </ul>
 * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/InstrTypes.h#L189">
 *     LLVM IR BinaryOperator Source</a>
 * @see <a href="https://llvm.org/docs/LangRef.html#binary-operations">
 *     LLVM LangRef: Binary Operations</a>
 * @see <a href="https://llvm.org/docs/LangRef.html#icmp-instruction">
 *     LLVM LangRef: icmp, fcmp</a>
 */
public class BinaryInst extends Instruction {

    //<editor-fold desc="Constructors">
    /**
     * User (Builder) needs to guarantee parameters passed correct.
     * @param type Type of operation result.
     * @param tag Instruction category.
     * @param lOp Left operand.
     * @param rOp Right operand.
     */
    public BinaryInst (Type type, InstCategory tag, Value lOp, Value rOp, BasicBlock bb) {
        super(type, tag, 2, bb);
        // Add left and right operands.
        this.addOperandAt(lOp, 0);
        this.addOperandAt(rOp, 1);
    }
    //</editor-fold>

    @Override
    public String toString() {
        // Build and return a string like "%1 = add i32 %2, %3"
        return
            // Result name
            this.getName() +
            " = " +
            // Operation code
            switch (this.cat) {
                case ADD -> "add i32 ";
                case SUB -> "sub i32 ";
                case MUL -> "mul i32 ";
                case DIV -> "sdiv i32 ";
                case LT -> "icmp slt " + this.getOperandAt(0).getType() + " ";
                case LE -> "icmp sle " + this.getOperandAt(0).getType() + " ";
                case GE -> "icmp sge " + this.getOperandAt(0).getType() + " ";
                case GT -> "icmp sgt " + this.getOperandAt(0).getType() + " ";
                case EQ -> "icmp eq " + this.getOperandAt(0).getType() + " ";
                case NE -> "icmp ne " + this.getOperandAt(0).getType() + " ";
                default -> "";
            } +
            // Left operand
            this.getOperandAt(0).getName() +
            ", " +
            // Right operand
            getOperandAt(1).getName();
    }

}
