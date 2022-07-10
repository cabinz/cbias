package backend.armCode.MCInstructions;

import backend.MCBuilder;
import backend.armCode.MCInstruction;
import backend.operand.MCOperand;

/**
 * In fact, this is not an ARM instruction. It's a MIR.
 */
public class MCReturn extends MCInstruction {

    @Override
    public String emit() {
        StringBuilder assemble = new StringBuilder();

//        /* stack balancing */
        int stackSize = belongFunc.getStackSize();
        if (stackSize > 0) {
            if (MCBuilder.canEncodeImm(stackSize))
                assemble.append("SUB sp, sp, #").append(stackSize).append("\n\t");
            else if (MCBuilder.canEncodeImm(-stackSize))
                assemble.append("ADD sp, sp, #").append(-stackSize).append("\n\t");
            else {
                int high16 = stackSize >>> 16;
                int low16 = stackSize & 0xFFFF;
                if (high16 == 0)
                    assemble.append("MOVW r5, #").append(low16).append("\n\t");
                else
                    assemble.append("MOVW r5, #").append(low16).append("\n\tMOVT r5, #").append(high16).append("\n\t");
            }
        }

        /* context switch */
        // TODO: Restore the physical register after register allocation
        if (belongFunc.useLR) {
            assemble.append("pop pc");
        }
        else
            assemble.append("bx lr");

        return assemble.toString();
    }

    public MCReturn(MCOperand returnValue) {
        super(TYPE.RET);
    }
}