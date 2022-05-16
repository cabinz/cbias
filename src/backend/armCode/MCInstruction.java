package backend.armCode;


import backend.operand.Register;

/**
 * This class represents all instructions of ARM in memory.
 */
public abstract class MCInstruction {

    public enum TYPE {
        ADD,
        SUB,
        RSB,//逆向减法
        MUL,
        DIV,
        AND,
        ORR,
        BIC,//位清零
        FMA,//乘加
        LongMUL,
        MOV,
        B,
        JMP,
        RETURN,
        STORE,
        LOAD,
        CMP,
        CALL,
        GLOBAL,
        COMMENT,
        PHI
    }


    //<editor-fold desc="Fields">
    private final TYPE type;
    private MCBasicBlock belongingBasicBlock;
    private MCFunction belongingFunction;
    private Shift shift;
    private ConditionField cond;
    //</editor-fold>


    abstract public String emit();


    //<editor-fold desc="Getter & Setter">
    public MCBasicBlock getBelongingBB() {return belongingBasicBlock;}
    public MCFunction getBelongingFunction() {return belongingFunction;}
    public Shift getShift() {return shift;}
    public ConditionField getCond() {return cond;}

    public void setBelongingBB(MCBasicBlock belongingBB) {this.belongingBasicBlock = belongingBB;}
    public void setBelongingFunction(MCFunction function) {this.belongingFunction = function;}
    public void setShift(Shift shift) {this.shift = shift;}
    public void setCond(ConditionField cond) {this.cond = cond;}
    //</editor-fold>


    //<editor-fold desc="Constructor">
    public MCInstruction(TYPE type, MCBasicBlock BasicBlock) {
        this.type = type;
        this.belongingBasicBlock = BasicBlock;
        this.belongingFunction = BasicBlock.getBelongingFunction();
    }
    public MCInstruction(TYPE type, MCBasicBlock belongingBB, Shift shift, ConditionField cond) {
        this.type = type;
        this.belongingBasicBlock = belongingBB;
        this.belongingFunction = belongingBB.getBelongingFunction();
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
        public Shift() {this.type = TYPE.NOPE;};
        public Shift(TYPE type, int imm) {this.type = type; this.immediate = imm;}
        public Shift(TYPE type, Register reg) {this.type = type; this.register = reg;}
        //</editor-fold>

    }

    /**
     * This class represents the condition field of each instruction in ARM.
     */
    public static class ConditionField {

        public enum TYPE {
            NOPE,
            EQ,
            NE,
            GE,
            GT,
            LE,
            LT
        }

        public TYPE type = TYPE.NOPE;

    }
}
