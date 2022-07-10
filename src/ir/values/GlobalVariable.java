package ir.values;

import ir.Type;
import ir.User;
import ir.types.PointerType;

/**
 * A GlobalVariable refers to a block of memory that can be determined at compilation time.
 * <br>
 * A global variable may have an initial value. Global Constants are required to have
 * initializers.
 * <br>
 * Type for GlobalVariable is PointerType of type of the memory block referenced.
 * @see <a href="https://llvm.org/docs/LangRef.html#global-variables">
 *     LLVM LangRef: Global Variable</a>
 * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/GlobalVariable.h#L40">
 *     LLVM IR Source: GlobalVariable</a>
 */
public class GlobalVariable extends User {

    //<editor-fold desc="Description">
    /**
     * If it is a global constant.
     */
    private boolean isGlbConst = false;

    /**
     * Represent the initial value specified in the statement.
     * <br>
     * The actual value of a non-constant glb var can be change
     * by other instructions, after which the initVal makes no
     * sense to this Value no more.
     */
    private Constant initVal;
    //</editor-fold>


    /**
     * Construct a GlbVar w/o initialization.
     * <br>
     * The initial value will automatically be assigned according to the type.
     * @param name The name of the GlbVar.
     * @param type The type of the memory block it references.
     */
    public GlobalVariable(String name, Type type) {
        super(PointerType.getType(type));
        this.setName("@" + name);
        if(type.isInteger()) {
            this.initVal = Constant.ConstInt.get(0);
        }
        else if(type.isArrayType()) {
            this.initVal = null;
        }
        // todo: float constant
    }

    /**
     * Construct a GlbVar with initialization.
     * <br>
     * The type will automatically be assigned according to
     * the given initial Constant.
     * @param name The name of the GlbVar.
     * @param init The initial value.
     */
    public GlobalVariable(String name, Constant init) {
        super(PointerType.getType(init.getType()));
        this.setName("@" + name);
        this.initVal = init;
    }

    public Constant getInitVal() {
        return initVal;
    }

    public boolean isArray() {
        return ((PointerType) getType()).getPointeeType().isArrayType();
    }

    /**
     * If it is a global constant.
     * @return True if it is. Otherwise, false.
     */
    public boolean isConstant() {
        return isGlbConst;
    }

    /**
     * Set the GlbVar to be a Constant.
     */
    public void setConstant() {
        this.isGlbConst = true;
    }

    /**
     * Get the type of the memory referenced by the GlobalVariable.
     * @return The type of the memory.
     */
    public Type getConstType() {
        return ((PointerType) this.getType()).getPointeeType();
    }

    @Override
    public String toString() {
        // e.g. "@a = dso_local [global | constant] i32 1"
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(this.getName()).append(" = dso_local ") // "@Glb = dso_local "
                .append(this.isConstant() ? "constant " : "global "); // "[global | constant] "

        // uninitialized array
        if (initVal == null) {
            Type arrType = ((PointerType) this.getType()).getPointeeType();
            strBuilder.append(arrType)
                    .append(" zeroinitializer");
        }
        else {
            strBuilder.append(initVal);
        }

        // todo: float type
        return strBuilder.toString();
    }
}
