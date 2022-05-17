package backend;

import backend.armCode.MCBasicBlock;
import backend.armCode.MCFunction;
import backend.armCode.MCInstruction;
import backend.armCode.MCInstructions.MCBinary;
import backend.armCode.MCInstructions.MCmov;
import backend.operand.*;
import ir.Module;
import ir.Value;
import ir.values.BasicBlock;
import ir.values.Constant;
import ir.values.Function;
import ir.values.Instruction;

import java.util.HashMap;

/**
 * This class is used to CodeGen, or translate LLVM IR into ARM assemble
 * (both are in memory). As the emitter, it's a Singleton.
 */
public class MCBuilder {

    //<editor-fold desc="Singleton Pattern">
    static private MCBuilder builder = new MCBuilder();

    private MCBuilder() {
        valueMap = new HashMap<>();
    };

    static public MCBuilder get() {return builder;}
    //</editor-fold>

    //<editor-fold desc="Fields">
    private Module IRModule;

    /**
     * This is used to name the virtual register.
     */
    private int VirtualRegCounter = 0;

    /**
     * This class records the map between values and virtual registers.
     */
    private HashMap<Value, VirtualRegister> valueMap;
    //</editor-fold>


    /**
     * Load LLVM IR module. Separate this process to support multi-module codegen.
     * (if possible?)
     * @param m IR module
     */
    public void loadModule(Module m) {IRModule = m;}


    /**
     * Translate LLVM IR to ARM assemble target. <br/>
     * <ul>
     *     <li>Map IR global variable into target global variable list</li>
     *     <li>Map IR function into target function list and Map IR BasicBlock into target function BasicBlock<</li>
     *     <li>Calculate loop info of function ( TO BE FINISHED )(为什么不放在pass)</li>
     *     <li>Find predecessors of a MCBasicBlock AND Calculate loop info of BasicBlock( TO BE FINISHED )(为什么不放在pass)( TO BE FINISHED )</li>
     *     <li>BFS travel the BasicBlock and then translate into ARM instruction()</li>
     *     <li>Handle PHI instruction( TO BE FINISHED )</li>
     *     <li>Calculate the cost of the MCInstruction( TO BE FINISHED )(为什么不放在pass)</li>
     * </ul>
     * @return generated ARM assemble target
     */
    public ARMAssemble codeGeneration() {
        ARMAssemble target = new ARMAssemble();
        mapGlobalVariable(IRModule, target);
        mapFunction(IRModule, target);

        return target;
    }

    private void mapGlobalVariable(Module IRModule, ARMAssemble target) {

    }

    /**
     * Map IR Function into MC Function and Add into assemble target
     * <ul>
     *     <li>Create MC Function</li>
     *     <li>Create MC BasicBlock for each BasicBlock in IR Function</li>
     *     <li>Translate BasicBlock</li>
     * </ul>
     */
    private void mapFunction(Module IRModule, ARMAssemble target) {
        for (Function IRfunc : IRModule.functions) {
            MCFunction MCfunc = target.createFunction(IRfunc);
            // TODO: 改成BFS
            for (BasicBlock IRBB : IRfunc.bbs){
                MCBasicBlock MCBB = MCfunc.createBB(IRBB);
                for (Instruction IRinst : IRBB) {
                    MCInstruction MCinst = translate(IRinst, MCBB, MCfunc);
                    MCBB.appendInstruction(MCinst);
                }
            }
        }
    }

    /**
     * 指令选择
     * @param IRinst IR instruction to be translated
     * @param MCBB The MC BasicBlock where the IR instruction belongs to
     * @param MCfunc The MC Function where the IR instruction belongs to
     * @return return the corresponding assembly instruction
     */
    private MCInstruction translate(Instruction IRinst, MCBasicBlock MCBB, MCFunction MCfunc) {
        if (IRinst.isRet()) {
            return translateRet(IRinst, MCBB);
        }
        else if (IRinst.isAdd()) {
            return translateAdd(IRinst, MCBB);
        }
        else if (IRinst.isSub()) {
            return translateSub(IRinst, MCBB);
        }
        else if (IRinst.isMul()) {
            return translateMul(IRinst, MCBB);
        }
        else if (IRinst.isDiv()) {
            return translateDiv(IRinst, MCBB);
        }
        else
            return null;
    }

    /**
     * Allocate a container or find the virtual register for a IR value.<br/>
     * What's a container? I consider a MC operand as a container. IR
     * value are hold in the immediate position, or register.
     * @param value the value to handle
     * @return the corresponding operand
     */
    private MCOperand findContainer(Value value) {
        // TODO: 识别立即数大小
        if (value instanceof Constant.ConstInt) {
            return new Immediate(((Constant.ConstInt) value).getVal());
        }
        else if (value instanceof Instruction) {
            if (valueMap.containsKey(value)) {
                return valueMap.get(value);
            }
            else {
                VirtualRegister vr = new VirtualRegister(VirtualRegCounter++, value);
                valueMap.put(value, vr);
                return vr;
            }
        }
        else
            return null;
    }

    private MCInstruction translateRet(Instruction IRinst, MCBasicBlock MCBB) {
        return new MCmov(MCBB, RealRegister.get(0), findContainer(IRinst.getOperandAt(0)));
    }

    private MCInstruction translateAdd(Instruction IRinst, MCBasicBlock MCBB) {
        MCOperand operand1 = findContainer(IRinst.getOperandAt(0));
        MCOperand operand2 = findContainer(IRinst.getOperandAt(1));
        if (operand1 instanceof Immediate) {
            if (operand2 instanceof Immediate) {
                VirtualRegister register = new VirtualRegister(VirtualRegCounter++, IRinst.getOperandAt(0));
                MCmov mov = new MCmov(MCBB, register, operand1);
                MCBB.appendInstruction(mov);
                return new MCBinary(MCInstruction.TYPE.ADD, MCBB, (Register) findContainer(IRinst), register, operand2);
            }
            else {
                return new MCBinary(MCInstruction.TYPE.ADD, MCBB, (Register) findContainer(IRinst), operand2, operand1);
            }
        }
        else {
            return new MCBinary(MCInstruction.TYPE.ADD, MCBB, (Register) findContainer(IRinst), operand1, operand2);
        }
    }

    private MCInstruction translateSub(Instruction IRinst, MCBasicBlock MCBB) {
        MCOperand operand1 = findContainer(IRinst.getOperandAt(0));
        MCOperand operand2 = findContainer(IRinst.getOperandAt(1));
        if (operand1 instanceof Immediate) {
            if (operand2 instanceof Immediate) {
                VirtualRegister register = new VirtualRegister(VirtualRegCounter++, IRinst.getOperandAt(0));
                MCmov mov = new MCmov(MCBB, register, operand1);
                MCBB.appendInstruction(mov);
                return new MCBinary(MCInstruction.TYPE.SUB, MCBB, (Register) findContainer(IRinst), register, operand2);
            }
            else {
                return new MCBinary(MCInstruction.TYPE.RSB, MCBB, (Register) findContainer(IRinst), operand2, operand1);
            }
        }
        else {
            return new MCBinary(MCInstruction.TYPE.SUB, MCBB, (Register) findContainer(IRinst), operand1, operand2);
        }
    }

    private MCInstruction translateMul(Instruction IRinst, MCBasicBlock MCBB) {
        MCOperand operand1 = findContainer(IRinst.getOperandAt(0));
        MCOperand operand2 = findContainer(IRinst.getOperandAt(1));
        if (operand1 instanceof Immediate) {
            if (operand2 instanceof Immediate) {
                VirtualRegister register = new VirtualRegister(VirtualRegCounter++, IRinst.getOperandAt(0));
                MCmov mov = new MCmov(MCBB, register, operand1);
                MCBB.appendInstruction(mov);
                return new MCBinary(MCInstruction.TYPE.MUL, MCBB, (Register) findContainer(IRinst), register, operand2);
            }
            else {
                return new MCBinary(MCInstruction.TYPE.MUL, MCBB, (Register) findContainer(IRinst), operand2, operand1);
            }
        }
        else {
            return new MCBinary(MCInstruction.TYPE.MUL, MCBB, (Register) findContainer(IRinst), operand1, operand2);
        }
    }

    // TODO: 处理除法
    private MCInstruction translateDiv(Instruction IRinst, MCBasicBlock MCBB) {
        return new MCBinary(MCInstruction.TYPE.DIV, MCBB, (Register) findContainer(IRinst), findContainer(IRinst.getOperandAt(0)), findContainer(IRinst.getOperandAt(1)));
    }


}
