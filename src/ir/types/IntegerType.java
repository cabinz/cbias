package ir.types;

import ir.Type;

/**
 * Class to represent integer types, including the built-in integer types: i1 and i32.
 * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/DerivedTypes.h#L40">
 *     LLVM IR Reference</a>
 */
public class IntegerType extends Type {

    //<editor-fold desc="Fields">
    /**
     * Field to represent the bit width of the integer type (1 or 32).
     */
    private int bitWidth;

    /**
     * Singleton for i32. Can be retrieved only by getI32().
     */
    private static final IntegerType i32 = new IntegerType(32);

    /**
     * Singleton for i32. Can be retrieved only by getI1().
     */
    private static final IntegerType i1 = new IntegerType(1);
    //</editor-fold>


    //<editor-fold desc="Constructors">
    private IntegerType(int bitWidth) {
        this.bitWidth = bitWidth;
    }
    //</editor-fold>


    //<editor-fold desc="Methods">
    public int getBitWidth() {
        return bitWidth;
    }

    /**
     * Get the singleton i32 type.
     * @return i32 type.
     */
    public static IntegerType getI32() {
        return i32;
    }

    /**
     * Get the singleton i1 type.
     * @return i1 type.
     */
    public static IntegerType getI1() {
        return i1;
    }

    @Override
    public String toString() {
        switch (bitWidth) {
            case 32:
                return "i32";
            case 1:
                return "i1";
            default:
                return "error";
        }
    }
    //</editor-fold>
}
