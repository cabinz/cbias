package ir.types;

import ir.Type;

import java.security.SecureRandom;

/**
 * Class to represent pointers.
 * Technically, each type should correspond to a unique instance (singleton) of
 * pointer type. But for convenience, PointerType is implemented in ordinary class
 * fashion without singleton method strategies.
 * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/DerivedTypes.h#L631">
 *     LLVM IR Source: PointerType</a>
 * @see <a href="https://llvm.org/docs/LangRef.html#pointer-type">
 *     LLVM LangRef: Pointer Type</a>
 */
public class PointerType extends Type {

    /**
     * Type of the pointed-to memory space. (one step forward on the pointing chain)
     * <br>
     * e.g. Root type of PtrType( [3 x i32]*** ) is PtrType( [3 x i32]** )
     */
    private Type pointeeType;

    public Type getPointeeType() {
        return pointeeType;
    }

    /**
     * Type of the ultimate element tracing up the pointing chain.
     * <br>
     * e.g. Root type of PtrType( [3 x i32]*** ) is ArrayType( [3 x i32] )
     */
    private Type rootType;

    public Type getRootType() {
        return rootType;
    }


    private PointerType(Type pointeeType) {
        this.pointeeType = pointeeType;

        if(pointeeType.isPointerType()) {
            rootType = ((PointerType) pointeeType).getRootType();
        }
        else {
            this.rootType = pointeeType;
        }
    }


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

    @Override
    public String toString() {
        // e.g. "i32*" and "i1*"
        return pointeeType.toString() + "*";
    }
}
