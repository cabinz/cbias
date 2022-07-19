package backend.armCode.MCInstructions;

import backend.MCBuilder;
import backend.armCode.MCInstruction;
import backend.operand.RealRegister;
import backend.operand.Register;

import java.util.HashSet;

/**
 * In fact, this is not an ARM instruction. It's a MIR.
 */
public class MCReturn extends MCInstruction {


    /**
     * The only use is LR
     */
    @Override
    public HashSet<Register> getUse() {
        var ret = new HashSet<Register>();
        if (!belongFunc.useLR)
            ret.add(RealRegister.get(14));
        return ret;
    }

    /**
     * Return instruction have NO  def! <br/>
     * This method should NEVER be called!
     */
    @Override
    public HashSet<Register> getDef() {return new HashSet<>();}

    @Override
    public void replaceRegister(Register old, Register tmp) {}

    @Override
    public String emit() {
        StringBuilder assemble = new StringBuilder();

//        /* stack balancing */
        int stackSize = belongFunc.getStackSize();
        if (stackSize > 0) {
            if (MCBuilder.canEncodeImm(stackSize))
                assemble.append("ADD sp, sp, #").append(stackSize).append("\n\t");
            else if (MCBuilder.canEncodeImm(-stackSize))
                assemble.append("SUB sp, sp, #").append(-stackSize).append("\n\t");
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
        if (belongFunc.useLR) {
            var restore = new HashSet<>(belongFunc.getContext());
            restore.remove(RealRegister.get(14));
            restore.add(RealRegister.get(15));
            assemble.append((new MCpop(restore)).emit());
        }
        else {
            if (!belongFunc.getContext().isEmpty())
                assemble.append((new MCpop(belongFunc.getContext())).emit()).append("\n\t");
            assemble.append("BX lr");
        }

        return assemble.toString();
    }

    public MCReturn() {
        super(TYPE.RET);
    }
}
