package backend.operand;

/**
 * This class represent a floating point immediate number in ARM assemble. <br/>
 * ARM limits the VMOV immediate can ONLY be <b>+/- m * 2^(-n)</b>, <br/>
 * which 16 <= m <= 31, 0 <= n <= 7
 */
public class FPImmediate extends MCOperand{

    private float floatValue;

    public float getFloatValue() {return floatValue;}
    public void setFloatValue(float floatValue) {this.floatValue = floatValue;}

    // TODO: 输出符合格式的浮点数
    public String emit() {return "#" + Float.toString(floatValue);}

    public FPImmediate(float floatValue) {
        super(TYPE.FP);
        this.floatValue = floatValue;
    }
}
