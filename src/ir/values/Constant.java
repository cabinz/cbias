package ir.values;

import ir.User;
import ir.Type;
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

    //<editor-fold desc="Constructors">
    public Constant(Type type) {
        super(type);
    }
    //</editor-fold>


    /**
     * Nested class ConstInt instantiates the IR of i32 integer constant in source.
     */
    public static class ConstInt extends Constant {
        //<editor-fold desc="Fields">
        /**
         * The mathematical value of the constant integer.
         */
        private int val;
        //</editor-fold>


        //<editor-fold desc="Getter & Setter">
        public int getVal() {return val;}
        //</editor-fold>


        //<editor-fold desc="Constructors">
        private ConstInt(Type type, int val) {
            super(type);
            this.val = val;
            this.setName(String.valueOf(val));
        }
        //</editor-fold>


        //<editor-fold desc="Methods">
        /**
         * Retrieve an IR Constant instance of given integer.
         * @param val Mathematical value of the integer.
         * @return Corresponding ConstInt instance created.
         */
        public static ConstInt get(int val) {
            return new ConstInt(IntegerType.getI32(), val);
        }

        @Override
        public String toString() {
            return "i32 " + this.val;
        }
        //</editor-fold>
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
        }

        /**
         * Retrieve an IR Constant instance of given float.
         * @param val Mathematical value of the float.
         * @return Corresponding ConstFloat instance created.
         */
        public ConstFloat get(float val) {
            return new ConstFloat(val);
        }
        //</editor-fold>

        @Override
        public String toString() {
            return this.getType() + " " + this.val;
        }
    }


    /**
     * Nested class for (integer/float) constant array in initialization.
     */
    public static class ConstArray extends Constant {

        /**
         * All the Constants in the arr will be the operands of the User.
         * @param type ArrayType for the array.
         * @param arr ArrayList of Constants for initializing the ConstArray.
         */
        public ConstArray(Type type, ArrayList<Constant> arr) {
            super(type);
            for (int i = 0; i < arr.size(); i++) {
                addOperandAt(arr.get(i), i);
            }
        }

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

    // todo float, and float array
}
