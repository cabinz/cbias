package ir.values;

import ir.User;
import ir.Type;
import ir.types.ArrayType;
import ir.types.FloatType;
import ir.types.IntegerType;

import java.util.ArrayList;

/**
 * A constant is a value that is immutable at runtime. Except ConstArray, all Constants are
 * implemented with real Factory Methods
 * <ul>
 *     <li>Integer and floating point values </li>
 *     <li>Arrays </li>
 *     <li>(Being referred in Global Variables)</li>
 * </ul>
 * Constant class has an operand list (inheriting from User), which is factually dedicated to ConstArray.
 * <br>
 * Type for a Constant is the type of that constant value.
 * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/Constant.h#L41">
 *     LLVM IR Reference</a>
 */
public abstract class Constant extends User {

    public Constant(Type type) {
        super(type);
    }

}
