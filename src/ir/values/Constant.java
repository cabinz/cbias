package ir.values;

import ir.User;
import ir.Type;
import ir.types.ArrayType;
import ir.types.FloatType;
import ir.types.IntegerType;

import java.util.ArrayList;

/**
 * A constant is a value that is immutable at runtime.
 * <br>
 * Though is reasonable to build a factory method for creating constant
 * on demand, Constant is implemented as normal class with public
 * constructor. Thus, there is NO guarantee that every value of constants
 * has only one Constant instance.
 * <ul>
 *     <li>Integer and floating point values </li>
 *     <li>Arrays </li>
 *     <li>Functions (Cuz their address is immutable) ?</li>
 *     <li>Global variables </li>
 * </ul>
 * All IR classes above are nested in Constant class.
 * Constant class has an operand list (inheriting from User).
 * <br>
 * Type for a Constant is the type of that constant value.
 * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/Constant.h#L41">
 *     LLVM IR Reference</a>
 */
public class Constant extends User {

    public Constant(Type type) {
        super(type);
    }

    /**
     * Nested class ConstInt instantiates the IR of i32 integer constant in source.
     */
    public static class ConstInt extends Constant {
        /**
         * The mathematical value of the constant integer.
         */
        private int val;
        public int getVal() {return val;}


        //<editor-fold desc="Factory Method">
        private ConstInt(Type type, int val) {
            super(type);
            this.val = val;
            this.setName(String.valueOf(val));
        }

        /**
         * Retrieve an IR Constant instance of given integer.
         * @param val Mathematical value of the integer.
         * @return Corresponding ConstInt instance created.
         */
        public static ConstInt get(int val) {
            return new ConstInt(IntegerType.getI32(), val);
        }
        //</editor-fold>

        @Override
        public String toString() {
            return this.getType() + " " + this.getName();
        }
    }


    public static class ConstFloat extends Constant {
        /**
         * The mathematical value of the constant float.
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
         * @param val Mathematical value of the float.
         * @return Corresponding ConstFloat instance created.
         */
        public static ConstFloat get(float val) {
            return new ConstFloat(val);
        }
        //</editor-fold>

        @Override
        public String toString() {
            return this.getType() + " " + this.getName();
        }
    }


    /**
     * Nested class for (integer/float) constant array in initialization.
     * <br>
     * All the Constants in the array will be the operands of it.
     */
    public static class ConstArray extends Constant {

        //<editor-fold desc="Factory Method">
        /**
         * Construct a constant array.
         * @param arrType ArrayType for the array.
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
         * @param arrType The ArrayType.
         * @param initList ArrayList of Constants in a same Type.
         *                 The length of it needs to match with the arrType.
         * @return The ConstArray instance required.
         */
        public static ConstArray get(ArrayType arrType, ArrayList<Constant> initList) {
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
            return new ConstArray(arrType, initList);
        }
        //</editor-fold>

        @Override
        public String toString() {
            StringBuilder strBuilder = new StringBuilder();
            strBuilder.append(this.getType()).append(" [");
            for(int i = 0; i < this.getNumOperands(); i++) {
                strBuilder.append(this.getOperandAt(i));
                if(i < this.getNumOperands() - 1){
                    strBuilder.append(", ");
                }
            }
            strBuilder.append("]");
            return strBuilder.toString();
        }
    }
}
