package backend.operand;

public class Label extends MCOperand {

    private String name;

    @Override
    public String emit() {
        return name;
    }

    public Label(String name) {
        super(TYPE.GBV);
        this.name = name;
    }
}
