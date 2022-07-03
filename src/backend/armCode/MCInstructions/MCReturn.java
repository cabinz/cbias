package backend.armCode.MCInstructions;

import backend.armCode.MCInstruction;
import backend.operand.MCOperand;

/**
 * In fact, this is not an ARM instruction. It's a MIR.
 */
public class MCReturn extends MCInstruction {

    private MCOperand returnValue;

    @Override
    public String emit() {
        // TODO
        return "RET " + returnValue.emit();
    }

    public MCReturn(MCOperand returnValue) {
        super(TYPE.RET);
        this.returnValue = returnValue;
    }
}
