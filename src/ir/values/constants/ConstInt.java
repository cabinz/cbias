package ir.values.constants;

import ir.Type;
import ir.types.IntegerType;
import ir.values.Constant;

/**
 * Class ConstInt instantiates the IR of i32 integer constant in source.
 */
public class ConstInt extends Constant {
    /**
     * The arithmetic value of the constant integer.
     */
    private int val;

    public int getVal() {
        return val;
    }


    //<editor-fold desc="Factory Method">
    private ConstInt(Type type, int val) {
        super(type);
        this.val = val;
        this.setName(String.valueOf(val));
    }

    /**
     * Retrieve an i32 Constant instance of given integer.
     *
     * @param val Mathematical value of the integer (i32).
     * @return Corresponding ConstInt instance created.
     */
    public static ConstInt get(int val) {
        return new ConstInt(IntegerType.getI32(), val);
    }

    /**
     * Retrieve an i1 Constant instance of given boolean.
     *
     * @param val Mathematical value of the integer (i1).
     * @return Corresponding ConstInt instance created.
     */
    public static ConstInt getI1(int val) {
        return new ConstInt(IntegerType.getI1(), val);
    }
    //</editor-fold>

    @Override
    public String toString() {
        return this.getType() + " " + this.getName();
    }
}
