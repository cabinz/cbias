package backend.operand;

import java.util.ArrayList;

/**
 * This label is created for global variable, not for the basic block or other
 */
public class Label extends MCOperand {

    private final String name;
    private final ArrayList<Integer> intial;

    public boolean isArray() {return intial.size() != 1;}

    public ArrayList<Integer> getIntial() {return intial;}

    @Override
    public String emit() {
        return name;
    }

    public Label(String name, ArrayList<Integer> initial) {
        super(TYPE.GBV);
        this.name = name;
        this.intial = initial;
    }
}
