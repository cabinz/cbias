package backend.operand;

import ir.Value;

public class VirtualRegister extends Register{

    private int name;

    /**
     * This field represents the IR value stored in.
     */
    private Value value;

    @Override
    public String getName() {return Integer.toString(name);}

    public String emit() {return "VReg_" + getName();}

    public VirtualRegister(int name, Value value) {
        super(TYPE.VTR);
        this.name = name;
        this.value = value;
    }
}
