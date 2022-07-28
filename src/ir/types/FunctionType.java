package ir.types;

import ir.Type;

import java.util.ArrayList;

/**
 * Class to represent function types, containing the prototype of a function
 * including its return type and argument type(s).
 * <br>
 * Technically, each prototype should correspond to a unique instance (singleton) of
 * FunctionType. But for convenience, FunctionType is implemented in ordinary class
 * fashion without singleton strategies.
 * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/DerivedTypes.h#L102">
 *     LLVM IR Source: FunctionType</a>
 * @see <a href="https://llvm.org/docs/LangRef.html#function-type">
 *     LLVM LangRef: Function Type</a>
 */
public class FunctionType extends Type {

    /**
     * Return type of the function. (VoidType / IntegerType / FloatType)
     */
    private Type retType;

    public Type getRetType() {
        return retType;
    }

    /**
     * Argument types in the function prototype.
     */
    private ArrayList<Type> argTypes;

    public ArrayList<Type> getArgTypes() {
        return argTypes;
    }



    private FunctionType(Type retType, ArrayList<Type> argTypes) {
        this.retType = retType;
        this.argTypes = argTypes;
    }


    /**
     * Just a wrapper of the private constructor of FunctionType,
     * for the sake of interface consistency of retrieving type
     * instances in Type system.
     * @param retType Type of the value returned by the function.
     * @param argTypes List of types of function arguments.
     * @return The prototype of the Function.
     */
    public static FunctionType getType(Type retType, ArrayList<Type> argTypes) {
        return new FunctionType(retType, argTypes);
    }

}
