package ir;

import ir.types.FunctionType;
import ir.types.IntegerType;

/**
 * Each Value instance has a type field containing type information related to it as an IR component.
 * <br>
 * The instances of the Type class are immutable: once they are created, they are never changed.
 * Only one instance of a particular type is ever created. To enforce this, all Type instances
 * exist in singleton fashion.
 * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/Type.h#L45">
 *     LLVM IR Type Source</a>
 * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/DerivedTypes.h">
 *     LLVM IR Derived Types Source</a>
 */
public class Type {

    //<editor-fold desc="Innerclass">
    /**
     * Nested Class: Type for Value object with no type.
     */
    public static class NonType extends Type {
        private static NonType type = new NonType();

        private NonType() {}

        public static NonType getType() {
            return type;
        }
    }

    /**
     * Nested Class: Type for function returning void.
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
     * Type for BasicBlock.
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
    public boolean isNonType() {
        return (this instanceof NonType);
    }

    public boolean isVoidType() {
        return (this instanceof VoidType);
    }

    public boolean isLabelType() {
        return (this instanceof LabelType);
    }

    public boolean FunctionType() {
        return (this instanceof FunctionType);
    }

    public boolean isInteger() {
        return (this instanceof IntegerType);
    }

    public boolean isI1() {
        return this.isInteger() && ((IntegerType) this).getBitWidth() == 1;
    }
    //</editor-fold>

}
