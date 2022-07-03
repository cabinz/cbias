package backend;

import backend.armCode.*;
import backend.armCode.MCInstructions.*;
import backend.operand.*;
import ir.Module;
import ir.Type;
import ir.Value;
import ir.types.ArrayType;
import ir.types.PointerType;
import ir.values.*;
import ir.values.instructions.*;

import java.util.HashMap;
import java.util.List;

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

    /* Current MC function & basic block */
    private MCFunction curFunc;
    private MCBasicBlock curMCBB;

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
        for (GlobalVariable gv : IRModule.globalVariables)
            target.addGlobalVariable(gv);
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
            curFunc = target.createFunction(IRfunc);
            /* This loop is to create the MC basic block in the same order of IR */
            for (BasicBlock IRBB : IRfunc)
                curFunc.createBB(IRBB);
            // TODO: 改成BFS
            for (BasicBlock IRBB : IRfunc){
                curMCBB = curFunc.findMCBB(IRBB);
                for (Instruction IRinst : IRBB) {
                    translate(IRinst);
                }
            }
        }
    }

    /**
     * 指令选择总模块，该函数仅会向MCBB中插入指令，块间关联和其他关系描述由指令自身完成
     * @param IRinst IR instruction to be translated
     */
    private void translate(Instruction IRinst) {
        if (IRinst.isRet()) {
            translateRet((TerminatorInst.Ret) IRinst);
        }
        else if (IRinst.isAdd()) {
            translateBinary((BinaryInst) IRinst, MCInstruction.TYPE.ADD);
        }
        else if (IRinst.isSub()) {
            translateBinary((BinaryInst) IRinst, MCInstruction.TYPE.SUB);
        }
        else if (IRinst.isMul()) {
            translateBinary((BinaryInst) IRinst, MCInstruction.TYPE.MUL);
        }
        else if (IRinst.isDiv()) {
            translateBinary((BinaryInst) IRinst, MCInstruction.TYPE.SDIV);
        }
        else if (IRinst.isAlloca()) {
            translateAlloca((MemoryInst.Alloca) IRinst);
        }
        else if (IRinst.isStore()) {
            translateStore((MemoryInst.Store) IRinst);
        }
        else if (IRinst.isLoad()) {
            translateLoad((MemoryInst.Load) IRinst);
        }
        else if (IRinst.isCall()) {
            translateCall((CallInst) IRinst);
        }
        else if (IRinst.isBr()) {
            translateBr((TerminatorInst.Br) IRinst);
        }
        else if (IRinst.isGEP()) {
            translateGEP((GetElemPtrInst) IRinst);
        }
    }


    //<editor-fold desc="Tools">
    /**
     * Allocate a container or find the virtual register for a IR value.<br/><br/>
     * What's a container? I consider a MC operand as a container. IR
     * values are stored in the immediate position, or register.
     * @param value the value to handle
     * @param forceAllocReg force allocate a virtual register if true
     * @return the corresponding operand
     */
    private MCOperand findContainer(Value value, boolean forceAllocReg) {
        if (valueMap.containsKey(value)) {
            return valueMap.get(value);
        }
        else if (value instanceof Instruction) {
            VirtualRegister vr = new VirtualRegister(VirtualRegCounter++, value);
            valueMap.put(value, vr);
            return vr;
        }
        else if (value instanceof GlobalVariable) {
            VirtualRegister vr = new VirtualRegister(VirtualRegCounter++, value);
            valueMap.put(value, vr);
            curMCBB.appendInst(new MCMove(vr, target.findGlobalVar((GlobalVariable) value)));
            return vr;
        }
        else if (value instanceof Constant.ConstInt) {
            MCOperand temp = createConstInt(((Constant.ConstInt) value).getVal());
            if (temp instanceof Register) return temp;
            if (forceAllocReg){
                VirtualRegister vr = new VirtualRegister(VirtualRegCounter++, ((Constant.ConstInt) value).getVal());
                valueMap.put(value, vr);
                curMCBB.appendInst(new MCMove(vr, temp));
                return vr;
            }
            else
                return temp;
        }
        else
            return null;
    }

    /**
     * Syntactic sugar of {@link #findContainer(Value, boolean)}. <br/>
     * Default do not force allocate a virtual register. <br/>
     */
    private MCOperand findContainer(Value value) {
        return findContainer(value, false);
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
     * @return the created container, maybe register or immediate.
     * @see backend.MCBuilder#canEncodeImm(int)
     */
    private MCOperand createConstInt(int value){
        if (canEncodeImm(value))
            return new Immediate(value);
        else{
            VirtualRegister vr = new VirtualRegister(VirtualRegCounter++, value);
            // TODO: 可能要换成LDR？
            curMCBB.appendInst(new MCMove(vr, new Immediate(value)));
            return vr;
        }
    }

    /**
     * Map the IR icmp into ARM condition field.
     * @param IRinst icmp instruction
     * @return the corresponding ARM condition field
     */
    private MCInstruction.ConditionField mapToArmCond(BinaryInst IRinst) {
        return switch (IRinst.cat) {
            case EQ -> MCInstruction.ConditionField.EQ;
            case NE -> MCInstruction.ConditionField.NE;
            case GE -> MCInstruction.ConditionField.GE;
            case LE -> MCInstruction.ConditionField.LE;
            case GT -> MCInstruction.ConditionField.GT;
            case LT -> MCInstruction.ConditionField.LT;
            default -> null;
        };
    }

    /**
     * Reverse the ARM condition field.
     * @param cond ARM condition field to be reversed
     * @return the reversed result
     */
    private MCInstruction.ConditionField reverseCond(MCInstruction.ConditionField cond) {
        return switch (cond) {
            case EQ -> MCInstruction.ConditionField.NE;
            case NE -> MCInstruction.ConditionField.EQ;
            case GE -> MCInstruction.ConditionField.LT;
            case LE -> MCInstruction.ConditionField.GT;
            case GT -> MCInstruction.ConditionField.LE;
            case LT -> MCInstruction.ConditionField.GE;
            default -> null;
        };
    }
    //</editor-fold>


    //<editor-fold desc="Translate functions">
    /**
     * Translate IR Call instruction into ARM instruction. <br/>
     * Function Stack: parameter, LR & others, local variables<br/>
     * &emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp; high &emsp;&emsp;&emsp; -->> &emsp;&emsp;&emsp; low <br/>
     * (FP先不存了吧，自己知道就好) <br/>
     * @param IRinst IR call instruction
     */
    private void translateCall(CallInst IRinst) {
        int oprNum = IRinst.getNumOperands();
        /* Argument push */
        for (int i=oprNum; i>=1; i++) {
            if (i <= 4) {
                curMCBB.appendInst(new MCMove(RealRegister.get(i-1), findContainer(IRinst.getOperandAt(i))));
            }
            else {
                curMCBB.appendInst(new MCstore((Register) findContainer(IRinst.getOperandAt(i)), RealRegister.get(13), createConstInt(-4), true));
            }
        }
        /* Branch */
        curMCBB.appendInst(new MCbranch(target.findMCFunc((Function) IRinst.getOperandAt(0))));
        /* Stack balancing */
        if (oprNum > 4)
            curMCBB.appendInst(new MCBinary(MCInstruction.TYPE.ADD, RealRegister.get(13), RealRegister.get(13), createConstInt(4*oprNum-4)));
        /* Save result */
        curMCBB.appendInst(new MCMove((Register) findContainer(IRinst), RealRegister.get(0)));
    }

    private void translateRet(TerminatorInst.Ret IRinst) {
        if (IRinst.getNumOperands() != 0)
            curMCBB.appendInst(new MCMove(RealRegister.get(0), findContainer(IRinst.getOperandAt(0))));
    }

    /**
     * Translate the IR alloca instruction into ARM. <br/>
     * IR Alloca will be translated into "sp := sp - 4" and "MOV Vreg, sp".<br/>
     * To be optimized later....
     * @param IRinst IR instruction to be translated
     */
    private void translateAlloca(MemoryInst.Alloca IRinst) {
        int offset = 0;
        Type allocated = IRinst.getAllocatedType();
        if (allocated.isInteger() || allocated.isPointerType()) {
            offset = 4;
        }
        else if (allocated.isArrayType()) {
            offset = ((ArrayType) allocated).getSize() * 4;
        }
        curMCBB.appendInst(new MCBinary(MCInstruction.TYPE.SUB, RealRegister.get(13), RealRegister.get(13), createConstInt(offset)));
        curMCBB.appendInst(new MCMove((Register) findContainer(IRinst), RealRegister.get(13)));
    }

    /**
     * Translate the IR store instruction into ARM. <br/>
     * NOTE: The first operand must be a REGISTER!!
     * @param IRinst IR instruction to be translated
     */
    private void translateStore(MemoryInst.Store IRinst) {
        MCOperand src = findContainer(IRinst.getOperandAt(0), true);
        MCOperand addr = findContainer(IRinst.getOperandAt(1));
        curMCBB.appendInst(new MCstore((Register) src, addr));
    }

    /**
     * Translate IR load, just like its name.
     * @param IRinst IR instruction to be translated
     */
    private void translateLoad(MemoryInst.Load IRinst) {
        curMCBB.appendInst(new MCload((Register) findContainer(IRinst), findContainer(IRinst.getOperandAt(0))));
    }

    /**
     * Translate IR br instruction into ARM branch and a lot of condition calculate in front.
     * @param IRinst IR instruction to be translated
     */
    private void translateBr(TerminatorInst.Br IRinst) {
        if (IRinst.isCondJmp()) {
            if (IRinst.getOperandAt(0) instanceof Constant.ConstInt) {
                int cond = ((Constant.ConstInt) IRinst.getOperandAt(0)).getVal();
                if (cond == 0)
                    curMCBB.appendInst(new MCbranch(curMCBB.findMCBB((BasicBlock) IRinst.getOperandAt(2))));
                else
                    curMCBB.appendInst(new MCbranch(curMCBB.findMCBB((BasicBlock) IRinst.getOperandAt(1))));
            }
            else {
                MCInstruction.ConditionField cond = translateIcmp((BinaryInst) IRinst.getOperandAt(0));
                curMCBB.appendInst(new MCbranch(curFunc.findMCBB((BasicBlock) IRinst.getOperandAt(1)), cond));
            }
        }
        else {
            curMCBB.appendInst(new MCbranch(curMCBB.findMCBB((BasicBlock) IRinst.getOperandAt(0))));
        }
    }

    /**
     * Translate the IR icmp into a lot of ARM calculation.
     * @param icmp IR instruction to be translated
     * @return the corresponding ARM condition field of icmp
     */
    private MCInstruction.ConditionField translateIcmp(BinaryInst icmp, boolean saveResult) {
        Value value1 = icmp.getOperandAt(0);
        Value value2 = icmp.getOperandAt(1);
        if (value1 instanceof Instruction && ((Instruction) value1).isIcmp())
            translateIcmp((BinaryInst) value1, true);
        if (value2 instanceof Instruction && ((Instruction) value2).isIcmp())
            translateIcmp((BinaryInst) value2, true);

        MCOperand operand1;
        MCOperand operand2;
        MCInstruction.ConditionField armCond = mapToArmCond(icmp);
        if (value1 instanceof Constant.ConstInt && !(value2 instanceof Constant.ConstInt)){
            operand1 = findContainer(value2);
            operand2 = findContainer(value1);
            armCond = reverseCond(armCond);
        }
        else {
            operand1 = findContainer(value1, true);
            operand2 = findContainer(value2);
        }

        curMCBB.appendInst(new MCcmp((Register) operand1, operand2));

        if (saveResult) {
            curMCBB.appendInst(new MCMove((Register) findContainer(icmp), createConstInt(1), armCond));
            curMCBB.appendInst(new MCMove((Register) findContainer(icmp), createConstInt(0), reverseCond(armCond)));
        }

        return armCond;
    }

    /**
     * Syntactic sugar of {@link #translateIcmp(BinaryInst, boolean)} <br/>
     * Default do NOT save the result to a register.
     */
    private MCInstruction.ConditionField translateIcmp(BinaryInst icmp) {
        return translateIcmp(icmp, false);
    }

    /**
     * Translate IR binary expression instruction into ARM instruction. <br/>
     * NOTE: The first operand must be a REGISTER!!
     * @param IRinst IR instruction to be translated
     * @param type Operation type, ADD/SUB/MUL/SDIV or more?
     */
    private void translateBinary(BinaryInst IRinst, MCInstruction.TYPE type) {
        // TODO: Optimization
        MCOperand operand1 = findContainer(IRinst.getOperandAt(0));
        MCOperand operand2 = findContainer(IRinst.getOperandAt(1));
        if (operand1.isImmediate()) {
            if (operand2.isImmediate()) {
                VirtualRegister register = (VirtualRegister) findContainer(IRinst.getOperandAt(0), true);
                curMCBB.appendInst(new MCBinary(type, (Register) findContainer(IRinst), register, operand2));
            }
            else {
                if (type != MCInstruction.TYPE.SUB)
                    curMCBB.appendInst(new MCBinary(type, (Register) findContainer(IRinst),(Register)  operand2, operand1));
                else
                    curMCBB.appendInst(new MCBinary(MCInstruction.TYPE.RSB, (Register) findContainer(IRinst),(Register)  operand2, operand1));
            }
        }
        else {
            curMCBB.appendInst(new MCBinary(type, (Register) findContainer(IRinst),(Register) operand1, operand2));
        }
    }

    /**
     * Translate IR GEP. Calculate the address of an element. <br/>
     */
    private void translateGEP(GetElemPtrInst IRinst) {
        // 不能确定的elementType是否是一层指针
        Type elemetType = ((PointerType) IRinst.getOperandAt(0).getType()).getPointeeType();
        /* The number of GEP */
        int operandNum = IRinst.getNumOperands() - 1;
        /* The length of each dimension */
        List<Integer> lengths = null;
        if (elemetType.isArrayType())
            lengths = ((ArrayType) elemetType).getDimSize();

        /* Prepare, dst = addr + totalOffset */
        Register addr = (Register) findContainer(IRinst.getOperandAt(0));
        int totalOffset = 0;
        Register dst = (Register) findContainer(IRinst);

        for (int i=1; i<=operandNum; i++) {
            /* offset size of this level = index * scale */
            MCOperand index = findContainer(IRinst.getOperandAt(i));
            int scale = 4;
            if (lengths != null)
                for (int j=i-1; j<lengths.size(); j++)
                    scale *= lengths.get(j);

            if (index.isImmediate()) {
                int offset = scale * ((Immediate) index).getIntValue();
                if (i == operandNum) {
                    totalOffset += offset;
                    if (totalOffset == 0)
                        curMCBB.appendInst(new MCMove(dst, addr));
                    else
                        curMCBB.appendInst(new MCBinary(MCInstruction.TYPE.ADD, dst, addr, createConstInt(totalOffset)));
                    addr = dst;
                }
                else
                    totalOffset += offset;
            }
            else {
                // TODO
                System.out.println("出现操作立即数范围的寻址地址，报错");
            }
        }
    }
    //</editor-fold>

}