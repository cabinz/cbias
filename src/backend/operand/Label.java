package backend.operand;

public class Label extends MCOperand {

    private String name;
    private int val;

    private boolean isArray = false;

    public boolean isArray() {return isArray;}

    public int getVal() {
        return val;
    }

    @Override
    public String emit() {
        return name;
    }

    public Label(String name, int val) {
        super(TYPE.GBV);
        this.name = name;
        this.val = val;
    }
}
