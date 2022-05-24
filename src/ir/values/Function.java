package ir.values;

import ir.Value;
import ir.types.FunctionType;
import ir.Type;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A Function represents a single function/procedure in IR.
 * A function basically consists of an argument list and a
 * list of basic blocks as function body.
 * <br>
 * Type for a Function is FunctionType, which contains a
 * prototype of the function (a return type and a list of
 * argument types).
 * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/Function.h#L61">
 *     LLVM IR Function Source</a>
 */
public class Function extends Value implements Iterable<BasicBlock>{

    /**
     * Innerclass: Represents a FORMAL argument in a function call
     * (designating a memory block on the stack of invoked function).
     */
    public class FuncArg extends Value {

        //<editor-fold desc="Fields">
        private int pos;
        //</editor-fold>


        //<editor-fold desc="Constructors">
        public FuncArg(Type type, int pos) {
            super(type);
            this.pos = pos;
        }
        //</editor-fold>


        //<editor-fold desc="Methods">
        @Override
        public String toString() {
            // e.g. "i32 %arg"
            return this.type + " " + this.name;
        }
        //</editor-fold>
    }


    //<editor-fold desc="Fields">
    /**
     * List of formal arguments.
     */
    private final ArrayList<FuncArg> args = new ArrayList<>();

    /**
     * Basic blocks in the function.
     */
    private final LinkedList<BasicBlock> bbs = new LinkedList<>();

    /**
     * If it's an extern function whose prototype (declaration) is given
     * but definition has not been specified.
     * In our case, isExternal is only for functions supported by
     * the SysY runtime lib.
     */
    private boolean isExternal = false;
    //</editor-fold>


    //<editor-fold desc="Constructors">
    /**
     * Constructor for function declared in compile unit.
     * If the function is in runtime lib to be linked in, the isExternal flag should be
     * specified as true. Otherwise, it should be false.
     * @param type FunctionType with a return type and a list of formal argument types
     * @param isExternal For functions in runtime lib, it should be true.
     */
    public Function(FunctionType type, boolean isExternal) {
        super(type);
        this.isExternal = isExternal;

        // Add arguments into the args list.
        ArrayList<Type> ar = ((FunctionType)this.type).getArgTypes();
        for (int i = 0; i < ar.size(); i++) {
            args.add(new FuncArg(ar.get(i), i));
        }
    }
    //</editor-fold>


    //<editor-fold desc="Methods">

    public ArrayList<FuncArg> getArgs() {
        return args;
    }

    public boolean isExternal() {
        return isExternal;
    }

    /**
     * Get the entry basic block of the function.
     * @return Reference to the entry block.
     */
    public BasicBlock getEntryBB() {
        return bbs.getFirst();
    }

    /**
     * Add a new basic block into the function.
     * @param bb The block to be added.
     */
    public void addBB(BasicBlock bb) {
        bbs.add(bb);
    }

    @Override
    public String toString() {
        // e.g. "i32 @func(i32 arg1, i32 arg2)"

        StringBuilder strBuilder = new StringBuilder();
        // Name of the function.
        strBuilder.append(((FunctionType) this.type).getRetType())
                .append(" @")
                .append(this.name)
                .append("(");
        // Argument list.
        for(int i = 0; i < getArgs().size(); i++) {
            // For extern function declaration, only argument types need to be
            // printed in the argument list.
            if (this.isExternal()) {
                strBuilder.append(getArgs().get(i).type);
            }
            // For a local function definition, both types and names (register)
            // need to be printed.
            else {
                strBuilder.append(getArgs().get(i));
            }
            // The last argument needs no comma following it.
            if (i != getArgs().size() - 1) {
                strBuilder.append(", ");
            }
        }
        // A right parenthesis to close it.
        strBuilder.append(")");

        return strBuilder.toString();
    }

    @Override
    public Iterator<BasicBlock> iterator() {
        return bbs.iterator();
    }
    //</editor-fold>


}
