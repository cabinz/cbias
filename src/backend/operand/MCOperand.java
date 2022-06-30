package backend.operand;

/**
 * This class represents all the operand that may be used in an instruction,
 * include immediate value, real physical register or virtual register.
 */
public abstract class MCOperand {

    public enum TYPE {
        IMM,
        VTR,
        RLR,
        GBV
    }
    TYPE type;

    public boolean isImmediate  () {return type == TYPE.IMM;}
    public boolean isVirtualReg () {return type == TYPE.VTR;}
    public boolean isRealReg    () {return  type == TYPE.RLR;}
    public boolean isGlobalVar  () {return type == TYPE.GBV;}

    public MCOperand(TYPE type) {this.type = type;}

    abstract public String emit();
}