package backend;

import backend.armCode.MCBasicBlock;
import backend.armCode.MCFunction;
import backend.armCode.MCInstruction;
import backend.armCode.MCInstructions.*;
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
    }

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
            if (IRfunc.isExternal()) {
                target.useExternalFunction(IRfunc);
                continue;
            }
            MCFunction MCfunc = target.createFunction(IRfunc);
            // TODO: 改成BFS
            for (BasicBlock IRBB : IRfunc){
                MCBasicBlock MCBB = MCfunc.createBB(IRBB);
                for (Instruction IRinst : IRBB) {
//                    System.out.println(IRinst.toString());
                    translate(IRinst, MCBB);
                }
            }
        }
    }

    /**
     * 指令选择总模块
     * @param IRinst IR instruction to be translated
     * @param MCBB The MC BasicBlock where the IR instruction belongs to
     */
    private void translate(Instruction IRinst, MCBasicBlock MCBB) {
        if (IRinst.isRet()) {
            translateRet(IRinst, MCBB);
        }
        else if (IRinst.isAdd()) {
            translateBinary(IRinst, MCBB, MCInstruction.TYPE.ADD);
        }
        else if (IRinst.isSub()) {
            translateBinary(IRinst, MCBB, MCInstruction.TYPE.SUB);
        }
        else if (IRinst.isMul()) {
            translateBinary(IRinst, MCBB, MCInstruction.TYPE.MUL);
        }
        else if (IRinst.isDiv()) {
            translateBinary(IRinst, MCBB, MCInstruction.TYPE.DIV);
        }
        else if (IRinst.isAlloca()) {
            translateAlloca(IRinst, MCBB);
        }
        else if (IRinst.isStore()) {
            translateStore(IRinst, MCBB);
        }
        else if (IRinst.isLoad()) {
            translateLoad(IRinst, MCBB);
        }
        else if (IRinst.isCall()) {
            translateCall(IRinst, MCBB);
        }
    }

    /**
     * Allocate a container or find the virtual register for a IR value.<br/>
     * What's a container? I consider a MC operand as a container. IR
     * value are hold in the immediate position, or register.
     * @param value the value to handle
     * @param forceAllocReg force allocate a virtual register if true
     * @return the corresponding operand
     */
    private MCOperand findContainer(Value value, boolean forceAllocReg) {
        if (valueMap.containsKey(value)) {
            return valueMap.get(value);
        }
        else if (value instanceof Instruction || forceAllocReg) {
            VirtualRegister vr = new VirtualRegister(VirtualRegCounter++, value);
            valueMap.put(value, vr);
            return vr;
        }
        if (value instanceof Constant.ConstInt) {
            return createImmediate(((Constant.ConstInt) value).getVal());
        }
        else
            return null;
    }

    // TODO: 识别能否直接编码立即数
    private Immediate createImmediate(int value){
        return new Immediate(value);
    }

    /**
     * Translate IR Call instruction into ARM instruction. <br/>
     * Function Stack: LR, local variables, call variables <br/>
     * &emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp; high &emsp;&emsp;&emsp; -->> &emsp;&emsp;&emsp; low <br/>
     * (FP先不存了吧，自己知道就好) <br/>
     * @param IRinst IR call instruction
     * @param MCBB The MC BasicBlock where the IR instruction belongs to
     */
    private void translateCall(Instruction IRinst, MCBasicBlock MCBB) {
        int oprNum = IRinst.getNumOperands();
        for (int i=1; i<oprNum; i++) {
            if (i <= 4) {
                MCBB.appendInstruction(new MCmov(RealRegister.get(i-1), findContainer(IRinst.getOperandAt(i), false)));
            }
            else {
                MCBB.appendInstruction(new MCstore((Register) findContainer(IRinst.getOperandAt(i), false), RealRegister.get(13), createImmediate(-4), true));
            }
        }
        MCBB.appendInstruction(new MCbranch(IRinst.getOperandAt(0).getName(), true));
        MCBB.appendInstruction(new MCBinary(MCInstruction.TYPE.ADD, RealRegister.get(13), RealRegister.get(13), createImmediate(4*oprNum-4)));
        MCBB.appendInstruction(new MCmov((Register) findContainer(IRinst, false), RealRegister.get(0)));
    }

    private void translateRet(Instruction IRinst, MCBasicBlock MCBB) {
        MCBB.appendInstruction(new MCmov(RealRegister.get(0), findContainer(IRinst.getOperandAt(0), false)));
    }

    /**
     * Translate the IR alloca instruction into ARM. <br/>
     * IR Alloca will be translated into "sp := sp - 4" and "MOV Vreg, sp".<br/>
     * To be optimized later....
     */
    private void translateAlloca(Instruction IRinst, MCBasicBlock MCBB) {
        MCBB.appendInstruction(new MCBinary(MCInstruction.TYPE.SUB, RealRegister.get(13), RealRegister.get(13), createImmediate(4)));
        MCBB.appendInstruction(new MCmov((Register) findContainer(IRinst, false), RealRegister.get(13)));
    }

    /**
     * Translate the IR store instruction into ARM. <br/>
     * NOTE: The first operand must be a REGISTER!!
     * @param IRinst IR instruction to be translated
     * @param MCBB The MC BasicBlock where the IR instruction belongs to
     */
    private void translateStore(Instruction IRinst, MCBasicBlock MCBB) {
        MCOperand src = findContainer(IRinst.getOperandAt(0), false);
        MCOperand addr = findContainer(IRinst.getOperandAt(1), false);
        if (src instanceof Immediate) {
            VirtualRegister register = (VirtualRegister) findContainer(IRinst.getOperandAt(0), true);
            MCBB.appendInstruction(new MCmov(register, src));
            MCBB.appendInstruction(new MCstore(register, findContainer(IRinst.getOperandAt(1), false)));
        }
        else
            MCBB.appendInstruction(new MCstore((Register) findContainer(IRinst.getOperandAt(0), false), addr));
    }

    /**
     * translate IR load, just like its name.
     * @param IRinst IR instruction to be translated
     * @param MCBB The MC BasicBlock where the IR instruction belongs to
     */
    private void translateLoad(Instruction IRinst, MCBasicBlock MCBB) {
        MCBB.appendInstruction(new MCload((Register) findContainer(IRinst, false), findContainer(IRinst.getOperandAt(0), false)));
    }

    /**
     * Translate IR binary expression instruction into ARM instruction. <br/>
     * NOTE: The first operand must be a REGISTER!!
     * @param IRinst IR instruction to be translated
     * @param MCBB The MC BasicBlock where the IR instruction belongs to
     * @param type Operation type, ADD/SUB/MUL/SDIV or more?
     */
    private void translateBinary(Instruction IRinst, MCBasicBlock MCBB, MCInstruction.TYPE type) {
        // TODO: 处理除法
        MCOperand operand1 = findContainer(IRinst.getOperandAt(0), false);
        MCOperand operand2 = findContainer(IRinst.getOperandAt(1), false);
        if (operand1 instanceof Immediate) {
            if (operand2 instanceof Immediate) {
                VirtualRegister register = (VirtualRegister) findContainer(IRinst.getOperandAt(0), true);
                MCBB.appendInstruction(new MCmov(register, operand1));
                MCBB.appendInstruction(new MCBinary(type, (Register) findContainer(IRinst, false), register, operand2));
            }
            else {
                if (type != MCInstruction.TYPE.SUB)
                    MCBB.appendInstruction(new MCBinary(type, (Register) findContainer(IRinst, false),(Register)  operand2, operand1));
                else
                    MCBB.appendInstruction(new MCBinary(MCInstruction.TYPE.RSB, (Register) findContainer(IRinst, false),(Register)  operand2, operand1));
            }
        }
        else {
            MCBB.appendInstruction(new MCBinary(type, (Register) findContainer(IRinst, false),(Register) operand1, operand2));
        }
    }


}
