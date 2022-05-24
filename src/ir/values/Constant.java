package ir.values;

import ir.User;
import ir.Type;
import ir.types.IntegerType;

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

    // todo int array

    // todo float, and float array
}
