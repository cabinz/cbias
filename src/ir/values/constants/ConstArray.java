package ir.values.constants;

import ir.types.ArrayType;
import ir.values.Constant;

import java.util.ArrayList;

/**
 * Class for (integer/float) constant array in initialization.
 * <br>
 * All the Constants in the array will be the operands of it.
 */
public class ConstArray extends Constant {

    //<editor-fold desc="Factory Method">

    /**
     * Construct a constant array.
     *
     * @param arrType  ArrayType for the array.
     * @param initList ArrayList of Constants for initializing the ConstArray.
     */
    private ConstArray(ArrayType arrType, ArrayList<Constant> initList) {
        super(arrType);
        for (int i = 0; i < initList.size(); i++) {
            addOperandAt(initList.get(i), i);
        }
    }

    /**
     * Retrieve a constant array with a list of initial values (Constants).
     *
     * @param arrType  The ArrayType.
     * @param initList ArrayList of Constants in a same Type.
     *                 The length of it needs to match with the arrType.
     * @return The ConstArray instance required.
     */
    public static ir.values.constants.ConstArray get(ArrayType arrType, ArrayList<Constant> initList) {
        /*
        Security Checks.
         */
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

        /*
        Retrieve the instance and return it.
         */
        return new ir.values.constants.ConstArray(arrType, initList);
    }
    //</editor-fold>

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
}