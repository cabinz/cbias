package backend.operand;

/**
 * This class represents a real physical register in the CPU.
 * In ARM, there's 16 registers, in which only 1 to 12 can be used.
 * The 13th register is used as Stack Pointer, the 14th used as
 * Link Register, the 15th used as Program Counter, the 16th used
 * as CPSR.
 */
public class RealRegister extends Register{

    public enum NAME {
        R1,
        R2,
        R3,
        R4,
        R5,
        R6,
        R7,
        R8,
        R9,
        R10,
        R11,
        R12,
        SP,
        LR,
        PC,
        CPSR
    }
    private NAME name;

    @Override
    public String getName() {return name.toString();}

    public RealRegister(NAME name) {
        super(TYPE.RLR);
        this.name = name;
    }
}
