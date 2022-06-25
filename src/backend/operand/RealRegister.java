package backend.operand;

import java.util.ArrayList;

/**
 * This class represents a real physical register in the CPU.<br/>
 * In ARM, there's 16 registers, in which only 0 to 12 can be used.
 * The r13 register is used as Stack Pointer, the r14 used as
 * Link Register, the r15 used as Program Counter, the 16th used
 * as CPSR.
 */
public class RealRegister extends Register{

    private String name;

    @Override
    public String getName() {return name;}

    public String emit() {
        return name;
    }

    //<editor-fold desc="Singleton Pattern">
    private RealRegister(String name) {
        super(TYPE.RLR);
        this.name = name;
    }

    static private ArrayList<RealRegister> regs = new ArrayList<>();

    static {
        regs.add(new RealRegister("r0"));
        regs.add(new RealRegister("r1"));
        regs.add(new RealRegister("r2"));
        regs.add(new RealRegister("r3"));
        regs.add(new RealRegister("r4"));
        regs.add(new RealRegister("r5"));
        regs.add(new RealRegister("r6"));
        regs.add(new RealRegister("r7"));
        regs.add(new RealRegister("r8"));
        regs.add(new RealRegister("r9"));
        regs.add(new RealRegister("r10"));
        regs.add(new RealRegister("r11"));
        regs.add(new RealRegister("r12"));
        regs.add(new RealRegister("sp"));
        regs.add(new RealRegister("lr"));
        regs.add(new RealRegister("pc"));
        regs.add(new RealRegister("cpsr"));
    }

    static public RealRegister get(int i) {return regs.get(i);}
    //</editor-fold>
}
