package backend.armCode.MCInstructions;

import backend.armCode.MCInstruction;
import backend.operand.MCOperand;
import backend.operand.Register;

/**
 * This class represent the move operation of ARM. <br/>
 * It's an aggregation too, containing MOV, MOVW, MOVT, MNV.
 */
public class MCMove extends MCInstruction {

    // TODO: MVN: move negative

    private Register dst;
    private MCOperand src;

    public String emit(){
        if (src.isGlobalVar())
            return "MOVW" + emitCond() + ' ' + dst.emit() + ", :lower16:" + src.emit() + "\n\tMOVT" + emitCond() + ' ' + dst.emit() + ", :upper16:" + src.emit();
        return "MOV" + emitCond() + ' ' + dst.emit() + ", " + src.emit();
    }

    public MCMove(Register dst, MCOperand src) {super(TYPE.MOV); this.dst = dst; this.src = src;}
    public MCMove(Register dst, MCOperand src, ConditionField cond) {super(TYPE.MOV, null, cond); this.dst = dst; this.src = src;}
}
