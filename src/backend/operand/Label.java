package backend.operand;

import java.util.ArrayList;

/**
 * This label is created for global variable, not for the basic block or other
 */
public class Label extends MCOperand {

    public enum TAG {
        Int,
        Float
    }

    private final String name;
    private final TAG tag;
    private final ArrayList initial;

    public boolean isArray() {return initial.size() != 1;}

    public ArrayList getInitial() {return initial;}

    public boolean isInt() {return tag == TAG.Int;}

    @Override
    public String emit() {
        return name;
    }

    public Label(String name, TAG tag, ArrayList initial) {
        super(TYPE.GBV);
        this.name = name;
        this.tag = tag;
        this.initial = initial;
    }
}
