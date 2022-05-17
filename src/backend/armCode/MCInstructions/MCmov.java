package backend.armCode.MCInstructions;

import backend.armCode.MCBasicBlock;
import backend.armCode.MCInstruction;
import backend.operand.MCOperand;

public class MCmov extends MCInstruction {

    private MCOperand operand1;
    private MCOperand operand2;

    public String emit(){
        return "MOV " + operand1.emit() + ", " + operand2.emit();
    }

    public MCmov(MCBasicBlock BasicBlock, MCOperand operand1, MCOperand operand2) {super(TYPE.MOV, BasicBlock); this.operand1 = operand1; this.operand2 = operand2;}
    public MCmov(MCBasicBlock belongingBB, MCOperand operand1, MCOperand operand2, Shift shift, ConditionField cond) {super(TYPE.MOV, belongingBB, shift, cond); this.operand1 = operand1; this.operand2 = operand2;}
}
