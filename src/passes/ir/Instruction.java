package passes.ir;

/**
 * Instruction wrapper, which will be inherited to store extra information.
 */
public class Instruction {

    protected final ir.values.Instruction rawInstruction;

    public Instruction(ir.values.Instruction instruction){
        this.rawInstruction = instruction;
    }

    public ir.values.Instruction getRawInstruction() {
        return rawInstruction;
    }

}
