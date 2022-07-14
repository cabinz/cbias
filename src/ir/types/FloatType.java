package ir.types;

import ir.Type;

/**
 * Class representing the floating point type.
 * <br>
 * LLVM IR has to support various kinds of floating point types including float,
 * double, float128, x86_fp80 and etc., while in SysY float(32bit) is the only
 * type of bit pattern we need to support.
 * @see <a href="https://llvm.org/docs/LangRef.html#floating-point-types">
 *     LLVM LangRef: Floating Point Type</a>
 */
public class FloatType extends Type {

    //<editor-fold desc="Singleton">
    private FloatType() {}

    /**
     * The singleton of the FloatType.
     */
    private static final FloatType floatType = new FloatType();

    /**
     * Retrieve a FloatType.
     * @return float type
     */
    public static FloatType getType() {
        return floatType;
    }
    //</editor-fold>

    @Override
    public String toString() {
        return "float";
    }
}
