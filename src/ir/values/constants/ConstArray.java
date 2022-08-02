package ir.values.constants;

import ir.Use;
import ir.Value;
import ir.types.ArrayType;
import ir.types.PrimitiveType;
import ir.values.Constant;

import java.util.*;

/**
 * Class for (integer/float) constant array in initialization.
 * All the Constants in the array will be the operands of it.
 */
public class ConstArray extends Constant {

    //<editor-fold desc="Singleton">
    /**
     * Construct a constant array.
     * @param arrType  ArrayType for the array.
     * @param initList ArrayList of Constants for initializing the ConstArray.
     */
    private ConstArray(ArrayType arrType, ArrayList<Constant> initList) {
        super(arrType);

        // Add all none zero operands.
        for (int i = 0; i < initList.size(); i++) {
            addOperandAt(i, initList.get(i));
        }
    }

    private static class ConstArrKey {
        public final ArrayType arrType;
        public final ArrayList<Constant> initList;

        public ConstArrKey(ArrayType arrType, ArrayList<Constant> initList) {
            this.arrType = arrType;
            this.initList = initList;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConstArrKey that = (ConstArrKey) o;
            return Objects.equals(arrType, that.arrType) && Objects.equals(initList, that.initList);
        }

        @Override
        public int hashCode() {
            return Objects.hash(arrType, initList);
        }
    }

    private static final HashMap<ConstArrKey, ConstArray> pool = new HashMap<>();

    /**
     * Retrieve a constant array with a list of initial values (Constants).
     * A Singleton fashion interface, but work with a normal class constructor without
     * instance pooling.
     *
     * @param arrType  The ArrayType.
     * @param initList ArrayList of Constants in a same Type.
     *                 The length of it needs to match with the arrType.
     * @return The ConstArray instance required.
     */
    public static ConstArray get(ArrayType arrType, ArrayList<Constant> initList) {
        //<editor-fold desc="Security checks">

        // Check length.
        if (initList.size() == 0) {
            throw new RuntimeException("Try to retrieve a ConstArray with length of 0.");
        }
        if (initList.size() != arrType.getLen()) {
            throw new RuntimeException("Array Type length doesn't match the length of the init list.");
        }
        // Check type.
        for (Constant elem : initList) {
            if (arrType.getElemType() != elem.getType()) {
                throw new RuntimeException(
                        "Try to get a ConstArray with different types of constants in the initialized list."
                );
            }
        }

        //</editor-fold>

        /*
        Retrieve the instance and return it.
         */
        var key = new ConstArrKey(arrType, initList);
        if (pool.containsKey(key)) {
            return pool.get(key);
        }

        var newArr = new ConstArray(arrType, initList);
        pool.put(key, newArr);
        return newArr;
    }
    //</editor-fold>

    /**
     * Return an element in the ConstArray with given indices.
     * @param indices Indices indicating the position of the element.
     * @return The element retrieved, which can be Value object at any level of the array.
     */
    public Value getElemByIndex(Iterable<Integer> indices) {
        Value elem = this;
        for (Integer idx : indices) {
            if (elem.getType().isArrayType()) {
                elem = ((ConstArray) elem).getOperandAt(idx);
            }
            else {
                throw new RuntimeException("Depth of indies given is to large.");
            }
        }
        return elem;
    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(this.getType()).append(" [");
        for (int i = 0; i < this.getNumOperands(); i++) {
            strBuilder.append(this.getOperandAt(i));
            if (i < this.getNumOperands() - 1) {
                strBuilder.append(", ");
            }
        }
        strBuilder.append("]");
        return strBuilder.toString();
    }

    /**
     * Retrieve the ArrayType of the array.
     * @return ArrayType of the array.
     */
    @Override
    public ArrayType getType() {
        return (ArrayType) super.getType();
    }

    @Override
    public boolean isZero() {
        for (Use use : this.getOperands()) {
            if (!((Constant) use.getUsee()).isZero()) {
                return false;
            }
        }
        return true;
    }
}
