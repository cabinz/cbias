package backend.armCode;


import backend.operand.Register;

/**
 * This class represents all instructions of ARM in memory. <br/>
 * All instructions' constructor is about the text info, which will be emitted.
 * The relational info need to be set using setter.
 */
public abstract class MCInstruction {

    /**
     * The type of Machine Code instruction, which will be emitted directly!
     * No more translate!
     */
    public enum TYPE {
        ADD,
        SUB,
        RSB,
        MUL,
        SDIV,
        AND,
        ORR,
        MLA,
        MLS,
        LongMUL,
        MOV,
        RET,
        BRANCH,
        STORE,
        LOAD,
        PUSH,
        POP,
        CMP,
        VADD,
        VSUB,
        VMUL,
        VMLA,
        VMLS,
        VDIV,
        VCMP,
        VCVT,
        VMOV,
        VMRS,
        VLDR,
        VSTR
    }

    /**
     * The instance of this class represents the condition field of an instruction in ARM.
     */
    public enum ConditionField {
        NOPE,
        EQ,
        NE,
        GE,
        GT,
        LE,
        LT
    }


    //<editor-fold desc="Fields">
    /**
     * This field indicates the type of instruction.
     */
    protected final TYPE type;
    /**
     * The conditional field. <br/>
     * All instructions have this,
     */
    private ConditionField cond;
    /**
     * The shift field. <br/>
     * But in fact, not all instructions have this field.
     * So it's not reasonable to put it here. :(
     */
    protected Shift shift;

    protected MCBasicBlock belongBasicBlock;
    protected MCFunction belongFunc;
    //</editor-fold>


    abstract public String emit();

    protected String emitCond() {
        if (cond == ConditionField.NOPE || cond == null)
            return "";
        else
            return cond.name();
    }


    //<editor-fold desc="Getter & Setter">
    public MCBasicBlock getBelongBB() {return belongBasicBlock;}
    public MCFunction getBelongFunc() {return belongFunc;}
    public Shift getShift() {return shift;}
    public ConditionField getCond() {return cond;}

    public void setBelongBB(MCBasicBlock belongBB) {this.belongBasicBlock = belongBB;}
    public void setBelongFunc(MCFunction function) {this.belongFunc = function;}
    public void setShift(Shift shift) {this.shift = shift;}
    public void setCond(ConditionField cond) {this.cond = cond;}
    //</editor-fold>


    //<editor-fold desc="Constructor">
    /* The constructor only initializes the TEXT information of the instruction.
       The relation info must be set use setter.  */
    public MCInstruction(TYPE type) {
        this.type = type;
    }
    public MCInstruction(TYPE type, Shift shift, ConditionField cond) {
        this.type = type;
        this.shift = shift;
        this.cond = cond;
    }
    //</editor-fold>


    /**
     * This class represents the shift operation of the second operand in an instruction.
     */
    public static class Shift {

        public enum TYPE {
            NOPE,
            ASR,//算数右移
            LSR,//逻辑右移
            LSL,//逻辑左移
            ROR,//循环右移
            RRX //扩展循环右移
        }

        //<editor-fold desc="Fields">
        private TYPE type;
        private int immediate;
        private Register register;
        //</editor-fold>


        //<editor-fold desc="Getter & Setter">
        public TYPE getType() {return type;}
        public int getImmediate() {return immediate;}
        public Register getRegister() {return register;}

        public void setType(TYPE type) {this.type = type;}
        public void setImmediate(int immediate) {this.immediate = immediate;}
        public void setRegister(Register register) {this.register = register;}
        //</editor-fold>


        //<editor-fold desc="Constructor">
        public Shift(TYPE type, int imm) {this.type = type; this.immediate = imm;}
        public Shift(TYPE type, Register reg) {this.type = type; this.register = reg;}
        //</editor-fold>

    }
}
