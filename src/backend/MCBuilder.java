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
    static private final MCBuilder builder = new MCBuilder();

    private MCBuilder() {
        valueMap = new HashMap<>();
    }

    static public MCBuilder get() {return builder;}
    //</editor-fold>

    //<editor-fold desc="Fields">
    private Module IRModule;

    private ARMAssemble target;

    /**
     * This is used to name the virtual register.
     */
    private int VirtualRegCounter = 0;

    /**
     * This class records the map between values and virtual registers.
     */
    private final HashMap<Value, VirtualRegister> valueMap;
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
        target = new ARMAssemble();
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


    //<editor-fold desc="Tools">
    /**
     * Allocate a container or find the virtual register for a IR value.<br/><br/>
     * What's a container? I consider a MC operand as a container. IR
     * values are stored in the immediate position, or register.
     * @param value the value to handle
     * @param MCBB the MC basic block the value will be inserted
     * @param forceAllocReg force allocate a virtual register if true
     * @return the corresponding operand
     */
    private MCOperand findContainer(Value value, MCBasicBlock MCBB, boolean forceAllocReg) {
        if (valueMap.containsKey(value)) {
            return valueMap.get(value);
        }
        else if (value instanceof Instruction) {
            VirtualRegister vr = new VirtualRegister(VirtualRegCounter++, value);
            valueMap.put(value, vr);
            return vr;
        }
        else if (value instanceof Constant.ConstInt) {
            MCOperand temp = createConstInt(((Constant.ConstInt) value).getVal(), MCBB);
            if (temp instanceof Register) return temp;
            if (forceAllocReg){
                VirtualRegister vr = new VirtualRegister(VirtualRegCounter++, ((Constant.ConstInt) value).getVal());
                valueMap.put(value, vr);
                MCBB.appendInst(new MCmov(vr, temp));
                return vr;
            }
            else
                return temp;
        }
        else
            return null;
    }

    /**
     * Syntactic sugar of {@link backend.MCBuilder#findContainer(Value, MCBasicBlock, boolean)}. <br/>
     * Default do not force allocate a virtual register. <br/>
     */
    private MCOperand findContainer(Value value, MCBasicBlock MCBB) {
        return findContainer(value, MCBB, false);
    }


    /**
     * This function is used to determine whether a number can
     * be put into an immediate container. <br/><br/>
     * ARM can ONLY use 12 bits to represent an immediate, which is separated
     * into 8 bits representing number and 4 bits representing rotate right(ROR).
     * This means 'shifter_operand = immed_8 Rotate_Right (rotate_imm * 2)'. <br/>
     * @see <a href='https://www.cnblogs.com/walzer/archive/2006/02/05/325610.html'>ARM汇编中的立即数<a/> <br/>
     * ARM Architecture Reference Manual(ARMARM) P446.
     * @param n the to be determined
     * @return the result
     */
    private boolean canEncodeImm(int n) {
        for (int ror = 0; ror < 32; ror += 2) {
            /* checkout whether the highest 24 bits is all 0. */
            if ((n & ~0xFF) == 0) {
                return true;
            }
            /* n rotate left 2 */
            n = (n << 2) | (n >>> 30);
        }
        return false;
    }


    /**
     * Create a container for a constant INT.
     * @param value the INT value to be translated into an immediate
     * @param MCBB the MC basic block which the value will be inserted
     * @return the created container, maybe register or immediate.
     * @see backend.MCBuilder#canEncodeImm(int)
     */
    private MCOperand createConstInt(int value, MCBasicBlock MCBB){
        if (canEncodeImm(value))
            return new Immediate(value);
        else{
            VirtualRegister vr = new VirtualRegister(VirtualRegCounter++, value);
            // TODO: 可能要换成LDR？
            MCBB.appendInst(new MCmov(vr, new Immediate(value)));
            return vr;
        }
    }
    //</editor-fold>


    //<editor-fold desc="Translate functions">
    /**
     * Translate IR Call instruction into ARM instruction. <br/>
     * Function Stack: parameter, LR & others, local variables<br/>
     * &emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp; high &emsp;&emsp;&emsp; -->> &emsp;&emsp;&emsp; low <br/>
     * (FP先不存了吧，自己知道就好) <br/>
     * @param IRinst IR call instruction
     * @param MCBB The MC BasicBlock where the IR instruction belongs to
     */
    private void translateCall(Instruction IRinst, MCBasicBlock MCBB) {
        int oprNum = IRinst.getNumOperands();
        /* Argument push */
        for (int i=oprNum; i>=1; i++) {
            if (i <= 4) {
                MCBB.appendInst(new MCmov(RealRegister.get(i-1), findContainer(IRinst.getOperandAt(i), MCBB)));
            }
            else {
                MCBB.appendInst(new MCstore((Register) findContainer(IRinst.getOperandAt(i), MCBB), RealRegister.get(13), createConstInt(-4, MCBB), true));
            }
        }
        /* Branch */
        MCBB.appendInst(new MCbranch(target.findMCFunc((Function) IRinst.getOperandAt(0))));
        /* Stack balancing */
        if (oprNum > 4)
            MCBB.appendInst(new MCBinary(MCInstruction.TYPE.ADD, RealRegister.get(13), RealRegister.get(13), createConstInt(4*oprNum-4, MCBB)));
        /* Save result */
        MCBB.appendInst(new MCmov((Register) findContainer(IRinst, MCBB), RealRegister.get(0)));
    }

    private void translateRet(Instruction IRinst, MCBasicBlock MCBB) {
        if (IRinst.getNumOperands() != 0)
            MCBB.appendInst(new MCmov(RealRegister.get(0), findContainer(IRinst.getOperandAt(0), MCBB)));
    }

    /**
     * Translate the IR alloca instruction into ARM. <br/>
     * IR Alloca will be translated into "sp := sp - 4" and "MOV Vreg, sp".<br/>
     * To be optimized later....
     */
    private void translateAlloca(Instruction IRinst, MCBasicBlock MCBB) {
        MCBB.appendInst(new MCBinary(MCInstruction.TYPE.SUB, RealRegister.get(13), RealRegister.get(13), createConstInt(4, MCBB)));
        MCBB.appendInst(new MCmov((Register) findContainer(IRinst, MCBB), RealRegister.get(13)));
    }

    /**
     * Translate the IR store instruction into ARM. <br/>
     * NOTE: The first operand must be a REGISTER!!
     * @param IRinst IR instruction to be translated
     * @param MCBB The MC BasicBlock where the IR instruction belongs to
     */
    private void translateStore(Instruction IRinst, MCBasicBlock MCBB) {
        MCOperand src = findContainer(IRinst.getOperandAt(0), MCBB, true);
        MCOperand addr = findContainer(IRinst.getOperandAt(1), MCBB);
        MCBB.appendInst(new MCstore((Register) src, addr));
    }

    /**
     * translate IR load, just like its name.
     * @param IRinst IR instruction to be translated
     * @param MCBB The MC BasicBlock where the IR instruction belongs to
     */
    private void translateLoad(Instruction IRinst, MCBasicBlock MCBB) {
        MCBB.appendInst(new MCload((Register) findContainer(IRinst, MCBB), findContainer(IRinst.getOperandAt(0), MCBB)));
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
        MCOperand operand1 = findContainer(IRinst.getOperandAt(0), MCBB);
        MCOperand operand2 = findContainer(IRinst.getOperandAt(1), MCBB);
        if (operand1 instanceof Immediate) {
            if (operand2 instanceof Immediate) {
                VirtualRegister register = (VirtualRegister) findContainer(IRinst.getOperandAt(0), MCBB, true);
                MCBB.appendInst(new MCBinary(type, (Register) findContainer(IRinst, MCBB), register, operand2));
            }
            else {
                if (type != MCInstruction.TYPE.SUB)
                    MCBB.appendInst(new MCBinary(type, (Register) findContainer(IRinst, MCBB),(Register)  operand2, operand1));
                else
                    MCBB.appendInst(new MCBinary(MCInstruction.TYPE.RSB, (Register) findContainer(IRinst, MCBB),(Register)  operand2, operand1));
            }
        }
        else {
            MCBB.appendInst(new MCBinary(type, (Register) findContainer(IRinst, MCBB),(Register) operand1, operand2));
        }
    }
    //</editor-fold>

}