package passes.ir.gcm;

/**
 * Instruction with extra information.
 */
class Instruction extends passes.ir.Instruction {

    public Instruction(ir.values.Instruction instruction) {
        super(instruction);
    }

    /// Reschedule placement

    private BasicBlock earlyPlacement;
    private BasicBlock latePlacement;


    public BasicBlock getEarlyPlacement() {
        return earlyPlacement;
    }

    public void setEarlyPlacement(BasicBlock earlyPlacement) {
        this.earlyPlacement = earlyPlacement;
    }

    public BasicBlock getLatePlacement() {
        return latePlacement;
    }

    public void setLatePlacement(BasicBlock latePlacement) {
        this.latePlacement = latePlacement;
    }

}
