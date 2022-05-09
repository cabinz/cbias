package ir.values;

import ir.Value;
import ir.types.FunctionType;
import ir.Type;

import java.util.ArrayList;
import java.util.List;

public class Function extends Value {
    /**
     * Innerclass: Represent a function formal parameter.
     */
    public class FuncArg extends Value {
        /*
        Members.
         */
        private List<Value> bounds; // What's bounds?
        private int pos;

        /*
        Constructors.
         */
        public FuncArg(Type type, int pos) {
            super(type);
            this.pos = pos;
        }

        /*
        Methods.
         */
        public void setBounds(List<Value> bounds) {
            this.bounds = bounds;
        }

        @Override
        public String toString() {
            return this.type + " " + this.name; // Print in the formal arg list.
        }
    }


    //<editor-fold desc="Fields">
    /**
     * Argument list.
     */
    private final ArrayList<FuncArg> args = new ArrayList<>();

    /**
     * Basic blocks in the function.
     */
    public final ArrayList<BasicBlock> bbs = new ArrayList<>();
    //</editor-fold>


    //<editor-fold desc="Constructors">
    public Function(Type type) {
        super(type);

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

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        // Name of the function.
        strBuilder.append(((FunctionType) this.type).getRetType())
                .append(" @")
                .append(this.name)
                .append("(");
        // Argument list.
        for(int i = 0; i < getArgs().size(); i++) {
            strBuilder.append(getArgs().get(i));
            if (i != getArgs().size() - 1) {
                strBuilder.append(", ");
            }
        }
        // A right parenthesis to close it.
        strBuilder.append(")");

        return strBuilder.toString();
    }
    //</editor-fold>


}
