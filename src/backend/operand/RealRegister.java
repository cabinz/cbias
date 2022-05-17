package backend.operand;

import java.util.ArrayList;

/**
 * This class represents a real physical register in the CPU.<br/>
 * In ARM, there's 16 registers, in which only 1 to 12 can be used.
 * The 13th register is used as Stack Pointer, the 14th used as
 * Link Register, the 15th used as Program Counter, the 16th used
 * as CPSR.
 */
public class RealRegister extends Register{


    /**
     * 才不是为了以读才写成小写的呢！哼！
     */
    public enum NAME {
        r0,
        r1,
        r2,
        r3,
        r4,
        r5,
        r6,
        r7,
        r8,
        r9,
        r10,
        r11,
        sp,
        lr,
        pc,
        cpsr
    }
    private NAME name;

    @Override
    public String getName() {return name.name();}

    public String emit() {
        return name.name();
    }

    public RealRegister(NAME name) {
        super(TYPE.RLR);
        this.name = name;
    }
}
