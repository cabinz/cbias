package ir.values.instructions;

import ir.Type;
import ir.Value;
import ir.values.BasicBlock;
import ir.values.Instruction;

public class UnaryOpInst extends Instruction {
    /**
     * User (Builder) needs to guarantee parameters passed correct.
     * @param type Type of operation result.
     * @param tag Instruction category.
     * @param opd The operand Value.
     */
    public UnaryOpInst (Type type, Instruction.InstCategory tag, Value opd, BasicBlock bb) {
        super(type, tag, bb);
        // Add left and right operands.
        this.addOperandAt(opd, 0);
    }

    @Override
    public String toString() {
        // e.g. "%4 = fneg float %3"
        return
                this.getName() + " = " // "%4 = "
                // Operation code
                + switch (this.cat) {
                    case FNEG -> "fneg ";
                    default -> "";
                }
                // The operand
                + this.getOperandAt(0); // "float %3"
    }
}
