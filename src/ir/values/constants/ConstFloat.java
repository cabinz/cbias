package ir.values.constants;

import ir.types.FloatType;
import ir.values.Constant;

/**
 * Class ConstFloat instantiates the IR of float integer constant in source.
 */
public class ConstFloat extends Constant {
    /**
     * The arithmetic value of the constant float.
     */
    private float val;

    public float getVal() {
        return val;
    }

    //<editor-fold desc="Factory Method">
    private ConstFloat(float val) {
        super(FloatType.getType());
        this.val = val;
        this.setName("0x" + Long.toHexString(Double.doubleToLongBits((double) val)));
    }

    /**
     * Retrieve an IR Constant instance of given float.
     *
     * @param val Mathematical value of the float.
     * @return Corresponding ConstFloat instance created.
     */
    public static ir.values.constants.ConstFloat get(float val) {
        return new ir.values.constants.ConstFloat(val);
    }
    //</editor-fold>

    @Override
    public String toString() {
        return this.getType() + " " + this.getName();
    }
}
