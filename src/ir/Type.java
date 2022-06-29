package ir;

import ir.types.ArrayType;
import ir.types.FunctionType;
import ir.types.IntegerType;
import ir.types.PointerType;

/**
 * Each Value instance has a type field containing type information related to it as an IR component.
 * <br>
 * The instances of the Type class are immutable: once they are created, they are never changed.
 * Only one instance of a particular type is ever created. To enforce this, all most all Type instances
 * exist in singleton fashion. (There are also exceptions like FunctionType and PointerType for
 * the sake of implementation convenience)
 * <br>
 * In the case of SysY needed only a subset of LLVM IR type system, we categorize Types as below:
 * <ul>
 *     <li>Primitive Types: VoidType, IntegerType, FloatType, LabelType</li>
 *     <li>Composed Types: PointerType, FunctionType</li>
 * </ul>
 * However, the concepts of "First Class Types" and "Single Value Types" in LLVM Language Reference
 * are still useful for understanding the type system.
 * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/Type.h#L45">
 *     LLVM IR Source: Type</a>
 * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/DerivedTypes.h">
 *     LLVM IR Source: Derived Types</a>
 * @see <a href="https://llvm.org/docs/LangRef.html#type-system">
 *     LLVM LangRef: Type System</a>
 */
public class Type {

    //<editor-fold desc="Innerclass">
    /**
     * Nested Class: The void type does not represent any value and has no size.
     * e.g. Functions returning void has VoidType,
     * Instructions yielding no result has VoidType.
     * @see <a href="https://llvm.org/docs/LangRef.html#void-type">
     *     LLVM LangRef: Void Type</a>
     */
    public static class VoidType extends Type {
        private static VoidType type = new VoidType();

        private VoidType() {}

        public static VoidType getType() {
            return type;
        }

        @Override
        public String toString() {
            return "void";
        }
    }

    /**
     * The label type represents code labels.
     * Basically it's a type dedicated to BasicBlock.
     * @see <a href="https://llvm.org/docs/LangRef.html#label-type">
     *     LLVM LangRef: Label Type</a>
     */
    public static class LabelType extends Type {
        public static LabelType type = new LabelType();

        private LabelType() {}

        public static LabelType getType() {
            return type;
        }

        @Override
        public String toString() {
            return "label";
        }
    }
    //</editor-fold>


    //<editor-fold desc="Methods">
    public boolean isVoidType() {
        return (this instanceof VoidType);
    }

    public boolean isLabelType() {
        return (this instanceof LabelType);
    }

    public boolean isFunctionType() {
        return (this instanceof FunctionType);
    }

    public boolean isInteger() {
        return (this instanceof IntegerType);
    }

    public boolean isI1() {
        return this.isInteger() && ((IntegerType) this).getBitWidth() == 1;
    }

    public boolean isI32() {
        return this.isInteger() && ((IntegerType) this).getBitWidth() == 32;
    }

    public boolean isPointerType() {
        return (this instanceof PointerType);
    }
    public boolean isArrayType() {
        return (this instanceof ArrayType);
    }
    //</editor-fold>

}
