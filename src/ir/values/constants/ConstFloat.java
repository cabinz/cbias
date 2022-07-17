package ir.values.constants;

import ir.types.FloatType;
import ir.values.Constant;

import java.util.Objects;

import java.util.HashMap;

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

    // Instance pools.
    private static final HashMap<Float, ConstFloat> pool = new HashMap<>();

    /**
     * Retrieve an IR Constant instance of given float.
     * @param val Mathematical value of the float.
     * @return Corresponding ConstFloat instance created.
     */
    public static ConstFloat get(float val) {
        if (pool.containsKey(val)) {
            return pool.get(val);
        }
        else {
            var newInstance = new ConstFloat(val);
            pool.put(val, newInstance);
            return newInstance;
        }
    }
    //</editor-fold>


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConstFloat that)) return false;
        return Float.compare(that.val, val) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(val);
    }

    @Override
    public String toString() {
        return this.getType() + " " + this.getName();
    }
}
