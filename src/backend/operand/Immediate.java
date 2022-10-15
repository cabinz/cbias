package backend.operand;

/**
 * This class represent an immediate value of ARM assemble.
 */
public class Immediate extends MCOperand {

    private int intValue;

    //<editor-fold desc="Getter & Setter">
    public int getIntValue() {return intValue;}
    public void setIntValue(int intValue) {this.intValue = intValue;}
    //</editor-fold>

    /**
     * This function is used to determine whether a number can
     * be put into an immediate container. <br/><br/>
     * ARM can ONLY use 12 bits to represent an immediate, which is separated
     * into 8 bits representing number and 4 bits representing rotate right(ROR).
     * This means 'shifter_operand = immed_8 Rotate_Right (rotate_imm * 2)'. <br/>
     * @see <a href='https://www.cnblogs.com/walzer/archive/2006/02/05/325610.html'>ARM汇编中的立即数</a> <br/>
     * ARM Architecture Reference Manual(ARMARM) P446.
     * @param n the to be determined
     * @return the result
     */
    public static boolean canEncodeImm(int n) {
        for (int ror = 0; ror < 32; ror += 2) {
            /* checkout whether the highest 24 bits is all 0. */
            if ((n & ~0xFF) == 0) {
                return true;
            }
            /* n rotate left 2 */
            n = (n << 2) | (n >>> 30);
        }
        return false;
    }


    public String emit() {
        return "#" + intValue;
    }


    //<editor-fold desc="Constructor">
    public Immediate(int intValue) {
        super(TYPE.IMM);
        this.intValue = intValue;
    }
    //</editor-fold>
}
