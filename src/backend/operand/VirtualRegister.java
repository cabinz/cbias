package backend.operand;

public class VirtualRegister extends Register{

    private int name;

    @Override
    public String getName() {return Integer.toString(name);}

    public VirtualRegister(int name) {
        super(TYPE.VTR);
        this.name = name;
    }
}
