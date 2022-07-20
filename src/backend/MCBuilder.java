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
import ir.values.constants.ConstInt;
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

    private MCBuilder() {}

    static public MCBuilder get() {return builder;}
    //</editor-fold>

    //<editor-fold desc="Fields">
    private Module IRModule;

    private ARMAssemble target;

    /* Current MC function & basic block */
    private Function curIRFunc;
    private MCFunction curFunc;
    private MCBasicBlock curMCBB;

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
     *     <li>Map IR function into target function list and Map IR BasicBlock into target function BasicBlock</li>
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
        for (Function IRfunc : IRModule.externFunctions) {
            target.useExternalFunction(IRfunc);
        }

        for (Function IRfunc : IRModule.functions) {
            curIRFunc = IRfunc;
            valueMap = new HashMap<>();
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
            translateAddSub((BinaryOpInst) IRinst, MCInstruction.TYPE.ADD);
        }
        else if (IRinst.isSub()) {
            translateAddSub((BinaryOpInst) IRinst, MCInstruction.TYPE.SUB);
        }
        else if (IRinst.isMul()) {
            translateMul((BinaryOpInst) IRinst);
        }
        else if (IRinst.isDiv()) {
            translateSDiv((BinaryOpInst) IRinst);
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
        if (value instanceof ConstInt) {
            MCOperand temp = createConstInt(((ConstInt) value).getVal());
            if (temp.isVirtualReg())
                return temp;
            /* temp is immediate */
            else {
                if (forceAllocReg){
                    /* TODO: If force to allocate a register, should we create a new one or attempt to find one hold in VR? */
                        /* Create new one: more MOV instruction is created */
                        /* Find old one: Expand the live range of one VR, may cause SPILLING */
                    /* For now, try to create new one */
//                    if (valueMap.containsKey(value))
//                        return valueMap.get(value);
//                    else {
                        VirtualRegister vr = curFunc.createVirReg(((ConstInt) value).getVal());
                        valueMap.put(value, vr);
                        curMCBB.appendInst(new MCMove(vr, temp));
                        return vr;
//                    }
                }
                else
                    return temp;
            }
        }
        else if (valueMap.containsKey(value)) {
            return valueMap.get(value);
        }
        else if (value instanceof Instruction inst) {
            VirtualRegister vr;
            /* This is used to translate the ZExt instruction */
            /* Considering that all data used in competition is 32 bits, */
            /* ignore the ZExt instruction */
            /* and use the origin instruction's container */
            if (inst instanceof CastInst.ZExt zExt)
                vr = ((VirtualRegister) findContainer(zExt.getOperandAt(0)));
            else
                vr = curFunc.createVirReg(inst);

            valueMap.put(inst, vr);
            return vr;
        }
        else if (value instanceof GlobalVariable) {
            VirtualRegister vr = curFunc.createVirReg(value);
            valueMap.put(value, vr);
            curMCBB.appendInst(new MCMove(vr, target.findGlobalVar((GlobalVariable) value)));
            return vr;
        }
        else if (value instanceof Function.FuncArg && curIRFunc.getArgs().contains(value)) {
            // TODO: better way: 在spill的时候选择load源地址，不过运行时间没有区别
            VirtualRegister vr = curFunc.createVirReg(value);
            valueMap.put(value, vr);
            int pos = ((Function.FuncArg) value).getPos();
            MCBasicBlock entry = curFunc.getEntryBlock();
            if (pos < 4) {
                entry.prependInst(new MCMove(vr, RealRegister.get(pos)));
            }
            else {
                /* Considering that parameter should not be too many .... use Immediate directly here */
                var load = new MCload(vr, RealRegister.get(13), new Immediate((pos-4)*4));
                entry.prependInst(load);
                curFunc.addParamCal(load);
            }
            return vr;
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
    public static boolean canEncodeImm(int n) {
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
     * Determine whether a number is the integer power of two.
     */
    private boolean isPowerOfTwo(int n) {
        return (n & (n-1)) == 0;
    }

    /**
     * Calculate the result of log_2 n
     * @param n the integer power of 2
     * @return the result
     */
    private int log2(int n) {
        int ret = 0;
        while (n != 0){
            n = n >>> 1;
            ret++;
        }
        return ret-1;
    }


    /**
     * Create a container for a constant INT, may be immediate or register.
     * @param value the INT value to be translated into an immediate
     * @return the created container, maybe register or immediate.
     * @see backend.MCBuilder#canEncodeImm(int)
     */
    private MCOperand createConstInt(int value){
        if (canEncodeImm(value))
            return new Immediate(value);
        else{
            VirtualRegister vr = curFunc.createVirReg(value);
            curMCBB.appendInst(new MCMove(vr, new Immediate(value), true));
            return vr;
        }
    }

    /**
     * Map the IR icmp into ARM condition field.
     * @param IRinst icmp instruction
     * @return the corresponding ARM condition field
     */
    private MCInstruction.ConditionField mapToArmCond(BinaryOpInst IRinst) {
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
        };
    }
    //</editor-fold>


    //<editor-fold desc="Translate functions">
    /**
     * Translate IR Call instruction into ARM instruction. <br/>
     * r0-r3 are caller-saved registers, while r4-r12 are callee-saved registers. <br/>
     * Function stack (from high to low): parameter, context, local variables, spilled nodes <br/>
     * @param IRinst IR call instruction
     */
    private void translateCall(CallInst IRinst) {
        // TODO: 浮点参数传递使用s0-s3
        // 另外，前4个浮点和整型使用寄存器传递
        int oprNum = IRinst.getNumOperands();
        /* Argument push */
        for (int i=oprNum-1; i>=1; i--) {
            if (i <= 4) {
                curMCBB.appendInst(new MCMove(RealRegister.get(i-1), findContainer(IRinst.getOperandAt(i))));
            }
            else {
                // TODO: 可能要换成push
                curMCBB.appendInst(new MCstore((Register) findContainer(IRinst.getOperandAt(i), true), RealRegister.get(13), createConstInt(-4), true));
            }
        }
        /* Branch */
        curMCBB.appendInst(new MCbranch(target.findMCFunc((Function) IRinst.getOperandAt(0))));
        /* Stack balancing */
        if (oprNum > 5)
            curMCBB.appendInst(new MCBinary(MCInstruction.TYPE.ADD, RealRegister.get(13), RealRegister.get(13), createConstInt(4*oprNum-20)));
        /* Save result */
        curMCBB.appendInst(new MCMove((Register) findContainer(IRinst), RealRegister.get(0)));

        curFunc.setUseLR();
    }

    private void translateRet(TerminatorInst.Ret IRinst) {
        if (IRinst.getNumOperands() != 0)
            curMCBB.appendInst(new MCMove(RealRegister.get(0), findContainer(IRinst.getOperandAt(0))));
        curMCBB.appendInst(new MCReturn());
    }

    /**
     * Translate the IR alloca instruction into ARM. <br/>
     * IR Alloca will be translated into "VirReg := sp + offset_before".
     * And sp will be added in front of a procedure,
     * meaning that the stack of function is allocated at beginning,
     * and do NOT change in the procedure until a function call then balanced. <br/>
     * To be optimized later....
     * @param IRinst IR instruction to be translated
     */
    private void translateAlloca(MemoryInst.Alloca IRinst) {
        int offset = 0;
        Type allocated = IRinst.getAllocatedType();
        if (allocated.isIntegerType() || allocated.isPointerType()) {
            offset = 4;
        }
        else if (allocated.isArrayType()) {
            offset = ((ArrayType) allocated).getSize() * 4;
        }
        curMCBB.appendInst(new MCBinary(MCInstruction.TYPE.ADD, (Register) findContainer(IRinst), RealRegister.get(13), createConstInt(curFunc.getLocalVariable())));

        curFunc.addLocalVariable(offset);
    }

    /**
     * Translate the IR store instruction into ARM. <br/>
     * NOTE: The first operand must be a REGISTER!!
     * @param IRinst IR instruction to be translated
     */
    private void translateStore(MemoryInst.Store IRinst) {
        Register src = ((Register) findContainer(IRinst.getOperandAt(0), true));
        Register addr = ((Register) findContainer(IRinst.getOperandAt(1)));
        curMCBB.appendInst(new MCstore(src, addr));
    }

    /**
     * Translate IR load, just like its name.
     * @param IRinst IR instruction to be translated
     */
    private void translateLoad(MemoryInst.Load IRinst) {
        curMCBB.appendInst(new MCload((Register) findContainer(IRinst), ((Register) findContainer(IRinst.getOperandAt(0)))));
    }

    /**
     * Translate IR br instruction into ARM branch and a lot of condition calculate in front.
     * @param IRinst IR instruction to be translated
     */
    private void translateBr(TerminatorInst.Br IRinst) {
        if (IRinst.isCondJmp()) {
            if (IRinst.getOperandAt(0) instanceof ConstInt) {
                int cond = ((ConstInt) IRinst.getOperandAt(0)).getVal();
                if (cond == 0)
                    curMCBB.appendInst(new MCbranch(curMCBB.findMCBB((BasicBlock) IRinst.getOperandAt(2))));
                else
                    curMCBB.appendInst(new MCbranch(curMCBB.findMCBB((BasicBlock) IRinst.getOperandAt(1))));
            }
            else {
                MCInstruction.ConditionField cond = translateIcmp((BinaryOpInst) IRinst.getOperandAt(0));
                curMCBB.appendInst(new MCbranch(curFunc.findMCBB((BasicBlock) IRinst.getOperandAt(1)), cond));
                curMCBB.appendInst(new MCbranch(curFunc.findMCBB((BasicBlock) IRinst.getOperandAt(2)), reverseCond(cond)));
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
    private MCInstruction.ConditionField translateIcmp(BinaryOpInst icmp, boolean saveResult) {
        Value value1 = icmp.getOperandAt(0);
        Value value2 = icmp.getOperandAt(1);

        /* If there is cmp or zext instruction in operands */
        if (value1 instanceof BinaryOpInst biOp && biOp.isIcmp())
            translateIcmp(biOp, true);
        if (value2 instanceof BinaryOpInst biOp && biOp.isIcmp())
            translateIcmp(biOp, true);
        if (value1 instanceof CastInst.ZExt zExt)
            translateIcmp((BinaryOpInst) zExt.getOperandAt(0), true);
        if (value2 instanceof CastInst.ZExt zExt)
            translateIcmp((BinaryOpInst) zExt.getOperandAt(0), true);

        /* Translate */
        Register operand1;
        MCOperand operand2;
        MCInstruction.ConditionField armCond = mapToArmCond(icmp);
        if (value1 instanceof ConstInt && !(value2 instanceof ConstInt)){
            operand1 = (Register) findContainer(value2);
            operand2 = findContainer(value1);
            if (armCond != MCInstruction.ConditionField.EQ && armCond != MCInstruction.ConditionField.NE)
                armCond = reverseCond(armCond);
        }
        else {
            operand1 = (Register) findContainer(value1, true);
            operand2 = findContainer(value2);
        }
        curMCBB.appendInst(new MCcmp(operand1, operand2));

        /* Save result */
        if (saveResult) {
            curMCBB.appendInst(new MCMove((Register) findContainer(icmp), createConstInt(1), null, armCond));
            curMCBB.appendInst(new MCMove((Register) findContainer(icmp), createConstInt(0), null, reverseCond(armCond)));
        }

        return armCond;
    }

    /**
     * Syntactic sugar of {@link #translateIcmp(BinaryOpInst, boolean)} <br/>
     * Default do NOT save the result to a register.
     */
    private MCInstruction.ConditionField translateIcmp(BinaryOpInst icmp) {
        return translateIcmp(icmp, false);
    }

    /**
     * Translate IR binary expression instruction into ARM instruction. <br/>
     * NOTE: The first operand must be a REGISTER!!
     * @param IRinst IR instruction to be translated
     * @param type Operation type, ADD/SUB
     */
    private void translateAddSub(BinaryOpInst IRinst, MCInstruction.TYPE type) {
        Value value1 = IRinst.getOperandAt(0);
        Value value2 = IRinst.getOperandAt(1);

        /* If there is icmp instruction in operands */
        if (value1 instanceof CastInst.ZExt zExt)
            translateIcmp((BinaryOpInst) zExt.getOperandAt(0), true);
        if (value2 instanceof CastInst.ZExt zExt)
            translateIcmp((BinaryOpInst) zExt.getOperandAt(0), true);

        MCOperand operand1 = findContainer(value1);
        MCOperand operand2 = findContainer(value2);
        Register dst = (Register) findContainer(IRinst);

        /* Translate */
        if (operand1.isImmediate()) {
            if (operand2.isImmediate()) {
                /* This case should not happen, so no optimization here */
                VirtualRegister register = (VirtualRegister) findContainer(IRinst.getOperandAt(0), true);
                curMCBB.appendInst(new MCBinary(type, dst, register, operand2));
            }
            else {
                if (type != MCInstruction.TYPE.SUB)
                    curMCBB.appendInst(new MCBinary(type, dst, (Register) operand2, operand1));
                else {
                    if (((ConstInt) value1).getVal() == 0)
                        curMCBB.appendInst(new MCMove(dst, operand2));
                    else
                        curMCBB.appendInst(new MCBinary(MCInstruction.TYPE.RSB, dst, (Register) operand2, operand1));
                }
            }
        }
        else {
            if (operand2.isImmediate() && ((ConstInt) value2).getVal() == 0)
                curMCBB.appendInst(new MCMove(dst, operand1));
            else
                curMCBB.appendInst(new MCBinary(type, dst, (Register) operand1, operand2));
        }
    }

    private void translateMul(BinaryOpInst IRinst) {
        Value operand1 = IRinst.getOperandAt(0);
        Value operand2 = IRinst.getOperandAt(1);
        boolean op1IsConst = operand1 instanceof ConstInt;
        boolean op2IsConst = operand2 instanceof ConstInt;
        Register dst = (Register) findContainer(IRinst);

        /* If both is not const, that's fine */
        if (!op1IsConst && !op2IsConst) {
            Register mul1 = (Register) findContainer(operand1, true);
            Register mul2 = (Register) findContainer(operand2, true);

            curMCBB.appendInst(new MCBinary(MCInstruction.TYPE.MUL, dst, mul1, mul2));
        }
        /* If both const, fold */
        else if (op1IsConst && op2IsConst) {
            int result = ((ConstInt) operand1).getVal() * ((ConstInt) operand2).getVal();
            if (canEncodeImm(result)){
                curMCBB.appendInst(new MCMove(dst, new Immediate(result)));
            }
            else {
                curMCBB.appendInst(new MCMove(dst, new Immediate(result), true));
            }
        }
        /* If there is one const */
        else {
            Value v = operand1;
            Value c = operand2;
            if (operand1 instanceof Constant) {
                v = operand2;
                c = operand1;
            }
            Register mul = (Register) findContainer(v);

            int intVal = ((ConstInt) c).getVal();
            int abs = intVal>0 ?intVal :-intVal;

            /* Optimization: 2^n = LSL n, 2^n-1 = RSB LSL n, 2^n+1 = ADD LSL n */
            if (isPowerOfTwo(abs)) {
                MCInstruction.Shift shift = new MCInstruction.Shift(MCInstruction.Shift.TYPE.LSL, log2(abs));
                curMCBB.appendInst(new MCMove(dst, mul, shift, null));
                if (intVal < 0) {
                    curMCBB.appendInst(new MCBinary(MCInstruction.TYPE.RSB, dst, dst, createConstInt(0)));
                }
            }
            else if (isPowerOfTwo(abs + 1)) {
                MCInstruction.Shift shift = new MCInstruction.Shift(MCInstruction.Shift.TYPE.LSL, log2(abs));
                curMCBB.appendInst(new MCBinary(MCInstruction.TYPE.ADD, dst, mul, mul, shift, null));
                if (intVal < 0) {
                    curMCBB.appendInst(new MCBinary(MCInstruction.TYPE.RSB, dst, dst, createConstInt(0)));
                }
            }
            else if (isPowerOfTwo(abs - 1)) {
                MCInstruction.TYPE type = intVal<0 ?MCInstruction.TYPE.SUB : MCInstruction.TYPE.RSB;
                MCInstruction.Shift shift = new MCInstruction.Shift(MCInstruction.Shift.TYPE.LSL, log2(abs+1));
                curMCBB.appendInst(new MCBinary(type, dst, mul, mul, shift, null));
            }
            else {
                Register mul1 = (Register) findContainer(operand1, true);
                Register mul2 = (Register) findContainer(operand1, true);
                curMCBB.appendInst(new MCBinary(MCInstruction.TYPE.MUL, dst, mul1, mul2));
            }
        }
    }

    private void translateSDiv(BinaryOpInst IRinst) {
        // TODO: 常量除数转乘法
        Register operand1 = (Register) findContainer(IRinst.getOperandAt(0), true);
        Register operand2 = (Register) findContainer(IRinst.getOperandAt(1), true);
        Register dst = (Register) findContainer(IRinst);

        curMCBB.appendInst(new MCBinary(MCInstruction.TYPE.SDIV, dst, operand1, operand2));
    }

    /**
     * Translate IR GEP. Calculate the address of an element. <br/>
     * Each index's base address is the last address resolution.
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

        /* Prepare, dst = baseAddr + totalOffset */
        Register baseAddr = (Register) findContainer(IRinst.getOperandAt(0));
        int totalOffset = 0;
        Register dst = (Register) findContainer(IRinst);

        for (int i=1; i<=operandNum; i++) {
            /* offset size of this level = index * scale */
            MCOperand index = findContainer(IRinst.getOperandAt(i));
            int scale = 4;
            if (lengths != null)
                for (int j=i-1; j<lengths.size(); j++)
                    scale *= lengths.get(j);

            /* If index is an immediate, calculate address until next variable or the last operand */
            if (index.isImmediate()) {
                int offset = scale * ((Immediate) index).getIntValue();
                totalOffset += offset;
                if (i == operandNum) {
                    if (totalOffset == 0)
                        curMCBB.appendInst(new MCMove(dst, baseAddr));
                    else
                        curMCBB.appendInst(new MCBinary(MCInstruction.TYPE.ADD, dst, baseAddr, createConstInt(totalOffset)));
                }
            }
            /* If index is a variable */
            else {
                /* If the index before is immediate, calculate here */
                if (totalOffset != 0) {
                    curMCBB.appendInst(new MCBinary(MCInstruction.TYPE.ADD, dst, baseAddr, createConstInt(totalOffset)));
                    totalOffset = 0;
                    baseAddr = dst;
                }

                /* Handle the variable calculation */
                /* If the scale is the integer power of two, use the {@link MCInstruction#shift} */
                if (isPowerOfTwo(scale)) {
                    /* Considering the size of array, 5 bits immediate is enough */
                    var shift = new MCInstruction.Shift(MCInstruction.Shift.TYPE.LSL, log2(scale));
                    curMCBB.appendInst(new MCBinary(MCInstruction.TYPE.ADD, dst, baseAddr, index, shift, null));
                }
                else {
                    /* Create register for scale */
                    VirtualRegister vr = curFunc.createVirReg(scale);
                    curMCBB.appendInst(new MCMove(vr, new Immediate(scale), canEncodeImm(scale)));
                    curMCBB.appendInst(new MCFma(MCInstruction.TYPE.MLA, dst, ((Register) index), vr, baseAddr));
                }

                baseAddr = dst;
            }
        }
    }
    //</editor-fold>

}