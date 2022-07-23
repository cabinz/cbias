package ir.values.constants;

import ir.Type;
import ir.types.IntegerType;
import ir.values.Constant;

import java.util.Objects;

import java.util.HashMap;

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

    // Instance pools.
    private static HashMap<Integer, ConstInt> pool = new HashMap<>();
    private static final ConstInt i1One = new ConstInt(IntegerType.getI1(), 1);
    private static final ConstInt i1Zero = new ConstInt(IntegerType.getI1(), 0);

    /**
     * Retrieve an i32 Constant instance of given integer.
     *
     * @param val Mathematical value of the integer (i32).
     * @return Corresponding ConstInt instance created.
     */
    public static ConstInt getI32(int val) {
        if (pool.containsKey(val)) {
            return pool.get(val);
        }
        else {
            var newInstance = new ConstInt(IntegerType.getI32(), val);
            pool.put(val, newInstance);
            return newInstance;
        }

    }

    /**
     * Retrieve an i1 Constant instance of given boolean.
     *
     * @param val Mathematical value of the integer (i1).
     * @return Corresponding ConstInt instance created.
     */
    public static ConstInt getI1(int val) {
        switch (val) {
            case 1 -> {return i1One;}
            case 0 -> {return i1Zero;}
            default -> throw new RuntimeException("Try to get a i1 ConstInt with non binary value.");
        }
    }
    //</editor-fold>

    @Override
    public String toString() {
        return this.getType() + " " + this.getName();
    }
}
