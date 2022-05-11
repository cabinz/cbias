package ir.types;

import ir.Type;

import java.util.ArrayList;

/**
 * Class to represent function types, containing the prototype of a function
 * including its return type and argument type(s).
 * <br>
 * Technically, each prototype should correspond to a unique instance (singleton) of
 * FunctionType. But for convenience, FunctionType is implemented in ordinary class
 * fashion without singleton / factory method strategies.
 * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/DerivedTypes.h#L102">
 *     LLVM IR Source</a>
 * @see <a href="https://llvm.org/docs/LangRef.html#function-type">
 *     LLVM Language Reference: Function Type</a>
 */
public class FunctionType extends Type {
    //<editor-fold desc="Fields">
    private Type retType; // void / int / float
    private ArrayList<Type> argTypes;
    //</editor-fold>


    //<editor-fold desc="Constructors">
    private FunctionType(Type retType, ArrayList<Type> argTypes) {
        this.retType = retType;
        this.argTypes = argTypes;
    }
    //</editor-fold>


    //<editor-fold desc="Methods">

    /**
     * Just a wrapper of the private constructor of FunctionType,
     * for the sake of interface consistency of retrieving type
     * instances in Type system.
     * @param retType Type of the value returned by the function.
     * @param argTypes List of types of function arguments.
     * @return
     */
    public static FunctionType getType(Type retType, ArrayList<Type> argTypes) {
        return new FunctionType(retType, argTypes);
    }

    public Type getRetType() {
        return retType;
    }

    public ArrayList<Type> getArgTypes() {
        return argTypes;
    }
    //</editor-fold>
}
