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


}
