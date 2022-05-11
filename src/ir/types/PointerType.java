package ir.types;

import ir.Type;

import java.security.SecureRandom;

/**
 * Class to represent pointers.
 * Technically, each type should correspond to a unique instance (singleton) of
 * pointer type. But for convenience, PointerType is implemented in ordinary class
 * fashion without singleton / factory method strategies.
 * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/DerivedTypes.h#L631">
 *     LLVM IR Source: PointerType</a>
 * @see <a href="https://llvm.org/docs/LangRef.html#pointer-type">
 *     LLVM Lang Ref: Pointer Type</a>
 */
public class PointerType extends Type {

    //<editor-fold desc="Fields">
    /**
     * Type of the pointed-to memory space.
     */
    private Type pointeeType;
    //</editor-fold>


    //<editor-fold desc="Constructors">
    private PointerType(Type pointeeType) {
        this.pointeeType = pointeeType;
    }
    //</editor-fold>


    //<editor-fold desc="Methods">
    /**
     * Just a wrapper of the private constructor of PointerType,
     * for the sake of interface consistency of retrieving type
     * instances in Type system.
     * @param pointeeType Type of the pointed-to memory space.
     * @return FunctionType pointing to specified-type memory.
     */
    public static PointerType getType(Type pointeeType) {
        return new PointerType(pointeeType);
    }

    /**
     * Retrieve the type of the pointed-to memory space.
     * @return Type of the pointed-to memory space.
     */
    public Type getPointeeType() {
        return pointeeType;
    }

    @Override
    public String toString() {
        // e.g. "i32* " and "i1* "
        return pointeeType.toString() + "* ";
     }
    //</editor-fold>


}
