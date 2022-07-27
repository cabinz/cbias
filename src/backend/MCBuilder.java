package backend;

import backend.armCode.MCBasicBlock;
import backend.armCode.MCFunction;
import backend.armCode.MCInstruction;
import backend.armCode.MCInstructions.*;
import backend.operand.*;
import ir.Module;
import ir.Type;
import ir.Value;
import ir.types.ArrayType;
import ir.types.PointerType;
import ir.values.*;
import ir.values.constants.ConstFloat;
import ir.values.constants.ConstInt;
import ir.values.instructions.*;
import passes.ir.IBBRelationship;
import passes.ir.RelationAnalysis;

import java.util.*;

/**
 * This class is used to CodeGen, or translate LLVM IR into ARM assemble
 * (both are in memory). As the emitter, it's a Singleton.
 */
public class MCBuilder {

    class IRBlockInfo extends passes.ir.BasicBlock implements IBBRelationship<IRBlockInfo> {

        public LinkedList<IRBlockInfo> predecessors = new LinkedList<>();
        public IRBlockInfo trueBlock;
        public IRBlockInfo falseBlock;

        @Override
        public void addPreviousBasicBlock(IRBlockInfo previousBlock) {
            predecessors.add(previousBlock);
        }

        @Override
        public void setFollowingBasicBlocks(List<IRBlockInfo> followingBasicBlocks) {
            switch (followingBasicBlocks.size()) {
                case 1 -> trueBlock = followingBasicBlocks.get(0);
                case 2 -> {
                    trueBlock = followingBasicBlocks.get(0);
                    falseBlock = followingBasicBlocks.get(1);
                }
            }
        }

        public IRBlockInfo(BasicBlock rawBasicBlock) {
            super(rawBasicBlock);
        }
    }

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
    private HashMap<Value, VirtualExtRegister> floatValueMap;
    private Map<BasicBlock, IRBlockInfo> blockInfo;

    /**
     * Contains the floats that can be encoded in VMOV instruction.
     * @see backend.operand.FPImmediate
     */
    private static final HashSet<Float> immFloat = new HashSet<>();
    static {
        float t = 2.0f;
        for (int n=0; n<=7; n++) {
            t /= 2;
            for (int m=16; m<=31; m++) {
                float tmp = m * t;
                immFloat.add(tmp);
                immFloat.add(-tmp);
            }
        }
    }
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
     *     <li>Handle PHI instruction</li>
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
            floatValueMap = new HashMap<>();
            blockInfo = new HashMap<>();
            IRfunc.forEach(bb -> blockInfo.put(bb, new IRBlockInfo(bb)));
            RelationAnalysis.analysisBasicBlocks(blockInfo);
            curFunc = target.createFunction(IRfunc);

            /* This loop is to create the MC basic block in the same order of IR */
            for (BasicBlock IRBB : IRfunc)
                curFunc.createBB(IRBB);

            for (BasicBlock IRBB : IRfunc){
                curMCBB = curFunc.findMCBB(IRBB);
                for (Instruction IRinst : IRBB) {
                    translate(IRinst);
                }
            }

            translatePhi();

            /* Allocate function variable stack in front of procedure */
            MCBasicBlock entry = curFunc.getEntryBlock();
            int variableSize = curFunc.getLocalVariable();
            if (canEncodeImm(variableSize)) {
                entry.prependInst(new MCBinary(MCInstruction.TYPE.SUB, RealRegister.get(13), RealRegister.get(13), new Immediate(variableSize)));
            } else {
                VirtualRegister vr = curFunc.createVirReg(variableSize);
                entry.prependInst(new MCBinary(MCInstruction.TYPE.SUB, RealRegister.get(13), RealRegister.get(13), vr));
                entry.prependInst(new MCMove(vr, new Immediate(variableSize), true));
            }

            /* Adjust parameter loads' offset */
            curFunc.getParamCal().forEach(move -> {
                int new_offset =
                        ((Immediate) move.getSrc()).getIntValue()
                        + curFunc.getLocalVariable();
                move.setSrc(new Immediate(new_offset));
                if (!canEncodeImm(new_offset))
                    move.setExceededLimit();
            });
        }
    }

    /**
     * 指令选择总模块，该函数仅会向MCBB中插入指令，块间关联和其他关系描述由指令自身完成
     * @param IRinst IR instruction to be translated
     */
    private void translate(Instruction IRinst) {
        switch (IRinst.cat) {
            case RET    -> translateRet((TerminatorInst.Ret) IRinst);
            case ADD    -> translateAddSub((BinaryOpInst) IRinst, MCInstruction.TYPE.ADD);
            case SUB    -> translateAddSub((BinaryOpInst) IRinst, MCInstruction.TYPE.SUB);
            case MUL    -> translateMul((BinaryOpInst) IRinst);
            case DIV    -> translateSDiv((BinaryOpInst) IRinst);
            case ALLOCA -> translateAlloca((MemoryInst.Alloca) IRinst);
            case STORE  -> translateStore((MemoryInst.Store) IRinst);
            case LOAD   -> translateLoad((MemoryInst.Load) IRinst);
            case CALL   -> translateCall((CallInst) IRinst);
            case BR     -> translateBr((TerminatorInst.Br) IRinst);
            case GEP    -> translateGEP((GetElemPtrInst) IRinst);
            case FADD   -> translateFloatBinary((BinaryOpInst) IRinst, MCInstruction.TYPE.VADD);
            case FSUB   -> translateFloatBinary((BinaryOpInst) IRinst, MCInstruction.TYPE.VSUB);
            case FMUL   -> translateFloatBinary((BinaryOpInst) IRinst, MCInstruction.TYPE.VMUL);
            case FDIV   -> translateFloatBinary((BinaryOpInst) IRinst, MCInstruction.TYPE.VDIV);
            case FNEG   -> translateFloatNeg((UnaryOpInst) IRinst);
            case FPTOSI -> translateConvert((CastInst) IRinst, true);
            case SITOFP -> translateConvert((CastInst) IRinst, false);
        }
    }


    //<editor-fold desc="Tools">
    /**
     * Allocate a container or find the virtual register for a IR value.<br/><br/>
     * What's a container? I consider a MC operand as a container. IR
     * values are stored in the immediate position, or register.
     * @param value the value to handle
     * @param forceAllocReg force allocate a virtual register if true
     * @return the corresponding container (ONLY core register or integer immediate)
     */
    private MCOperand findContainer(Value value, boolean forceAllocReg) {
        if (value instanceof Constant) {
            int val = value instanceof ConstInt
                    ? ((ConstInt) value).getVal()
                    : Float.floatToRawIntBits(((ConstFloat) value).getVal());
            MCOperand temp = createConstInt(val);
            if (temp.isVirtualReg())
                return temp;
            /* temp is immediate */
            else {
                if (forceAllocReg){
                    /* TODO: If force to allocate a register, should we create a new one or attempt to find one hold in VR? */
                        /* Create new one: more MOV instruction is created */
                        /* Find old one: Expand the live range of one VR, may cause SPILLING, and must follow the CFG */
                    /* For now, try to create new one */
//                    if (valueMap.containsKey(value))
//                        return valueMap.get(value);
//                    else {
                        VirtualRegister vr = curFunc.createVirReg(val);
                        valueMap.put(value, vr);
                        curMCBB.appendInst(new MCMove(vr, temp));
                        return vr;
//                    }
                }
                else
                    return temp;
            }
        }
        else if (value instanceof GlobalVariable) {
            // TODO: 采用控制流分析，是否能访问到之前的地址
            VirtualRegister vr = curFunc.createVirReg(value);
            valueMap.put(value, vr);
            curMCBB.appendInst(new MCMove(vr, target.findGlobalVar((GlobalVariable) value)));
            return vr;
        }
        else if (valueMap.containsKey(value)) {
            return valueMap.get(value);
        }
        else if (floatValueMap.containsKey(value)) {
            var vr = curFunc.createVirReg(value);
            valueMap.put(value, vr);
            curMCBB.appendInst(new MCFPmove(vr, floatValueMap.get(value)));
            return vr;
        }
        else if (value instanceof Instruction) {
            var inst = ((Instruction) value);
            VirtualRegister vr;
            /* This is used to translate the ZExt instruction */
            /* Considering that all data used in competition is 32 bits, */
            /* ignore the ZExt instruction */
            /* and use the origin instruction's container */
            if (inst instanceof CastInst.ZExt)
                vr = ((VirtualRegister) findContainer(inst.getOperandAt(0)));
            else
                vr = curFunc.createVirReg(inst);

            valueMap.put(inst, vr);
            return vr;
        }
        else if (value instanceof Function.FuncArg && curIRFunc.getArgs().contains(value)) {
            // TODO: better way: 在spill的时候选择load源地址，不过运行时间没有区别
            // TODO: 使用到传参的时候才计算load
            VirtualRegister vr = curFunc.createVirReg(value);
            valueMap.put(value, vr);
            MCBasicBlock entry = curFunc.getEntryBlock();

            if (curFunc.getAPVCR().contains(value)) {
                entry.prependInst(new MCMove(vr, RealRegister.get(curFunc.getAPVCR().indexOf(value))));
            }
            else {
                int offsetVal = curFunc.getACTM().indexOf(value)*4;
                var offset = curFunc.createVirReg(offsetVal);
                entry.prependInst(new MCload(vr, RealRegister.get(13), offset));
                var move = new MCMove(offset, new Immediate(offsetVal), !canEncodeImm(offsetVal));
                entry.prependInst(move);
                curFunc.addParamCal(move);
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
     * Allocate a container for float value
     * @param value the value that needs a container
     * @param forceAllocReg need to force allocate an extension register for the value
     * @return the corresponding container (ONLY extension register or float immediate)
     */
    private MCOperand findFloatContainer(Value value, boolean forceAllocReg) {
        if (value instanceof Constant) {
            /* Find a float container for the const float (the format is IEEE 754 FLOAT) */
            if (value instanceof ConstFloat) {
                float v = ((ConstFloat) value).getVal();
                if (canEncodeFloat(v)) {
                    if (forceAllocReg) {
                        var extVr = curFunc.createExtVirReg(value);
                        curMCBB.appendInst(new MCFPmove(extVr, new FPImmediate(v)));
                        floatValueMap.put(value, extVr);
                        return extVr;
                    } else {
                        return new FPImmediate(v);
                    }
                }
                else {
                    var vr = curFunc.createVirReg(value);
                    var extVr = curFunc.createExtVirReg(value);
                    curMCBB.appendInst(new MCMove(vr, new FPImmediate(v), true));
                    curMCBB.appendInst(new MCFPmove(extVr, vr));
                    floatValueMap.put(value, extVr);
                    return extVr;
                }
            }
            /* Find a float container for the const int (the format is IEEE 754 INT!) */
            else {
                var vr = (Register) findContainer(value, true);
                var extVr = curFunc.createExtVirReg(value);
                curMCBB.appendInst(new MCFPmove(extVr, vr));
                return extVr;
            }
        }
        else if (floatValueMap.containsKey(value)) {
            return floatValueMap.get(value);
        }
        else if (valueMap.containsKey(value)) {
            var extVr = curFunc.createExtVirReg(value);
            floatValueMap.put(value, extVr);
            curMCBB.appendInst(new MCFPmove(extVr, valueMap.get(value)));
            return extVr;
        }
        else if (value instanceof Instruction) {
            var inst = ((Instruction) value);
            var vr = curFunc.createExtVirReg(inst);
            floatValueMap.put(value, vr);
            return vr;
        }
        else if (value instanceof Function.FuncArg && curIRFunc.getArgs().contains(value)) {
            VirtualExtRegister extVr = curFunc.createExtVirReg(value);
            floatValueMap.put(value, extVr);
            MCBasicBlock entry = curFunc.getEntryBlock();

            if (curFunc.getAPVER().contains(value)) {
                entry.prependInst(new MCFPmove(extVr, RealExtRegister.get(curFunc.getAPVER().indexOf(value))));
            }
            else {
                int offsetVal = curFunc.getACTM().indexOf(value)*4;
                var addr = curFunc.createVirReg(offsetVal);
                entry.prependInst(new MCFPload(extVr, addr));
                entry.prependInst(new MCBinary(MCInstruction.TYPE.ADD, addr, RealRegister.get(13), addr));
                var move = new MCMove(addr, new Immediate(offsetVal), !canEncodeImm(offsetVal));
                entry.prependInst(move);
                curFunc.addParamCal(move);
            }

            return extVr;
        }
        else
            return null;
    }

    /**
     * Syntactic sugar of {@link #findFloatContainer(Value, boolean)} <br/>
     * Default DO force allocate a virtual extension register
     * , because most float instruction use extension register
     * rather than immediate. <br/>
     */
    private MCOperand findFloatContainer(Value value) {
        return findFloatContainer(value, true);
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

    public static boolean canEncodeFloat(float n) {
        return immFloat.contains(n);
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
            case EQ, FEQ -> MCInstruction.ConditionField.EQ;
            case NE, FNE -> MCInstruction.ConditionField.NE;
            case GE, FGE -> MCInstruction.ConditionField.GE;
            case LE, FLE -> MCInstruction.ConditionField.LE;
            case GT, FGT -> MCInstruction.ConditionField.GT;
            case LT, FLT -> MCInstruction.ConditionField.LT;
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
     * r0-r3 & s0-s15 are caller-saved registers, via which the first 4 int & 16 float arguments will be passed. <br/>
     * The other arguments will be copied to memory, pushed in the reversed order. <br/>
     * Function stack (from high to low): parameter, context, spilled nodes, local variables <br/>
     * @param IRinst IR call instruction
     * @see <a href='https://web.eecs.umich.edu/~prabal/teaching/resources/eecs373/ARM-AAPCS-EABI-v2.08.pdf'>Procedure Call Standard for the ARM Architecture</a> <br/>
     * Charpter 5 The Base Procedure Call Standard & Charpter 6 The Standard Variants
     */
    private void translateCall(CallInst IRinst) {
        var callee = (Function) IRinst.getOperandAt(0);
        var MCCalee = target.findMCFunc(callee);
        var args = callee.getArgs();
        var APVCR = MCCalee.getAPVCR();
        var APVER = MCCalee.getAPVER();
        var ACTM = MCCalee.getACTM();

        /* Generate Instruction */
        int stackSize = ACTM.size();
        for (int i = IRinst.getNumOperands()-1; i > 0; i--) {
            var param = IRinst.getOperandAt(i);
            var crspArg = args.get(i-1);
            if (ACTM.contains(crspArg)) {
                if (param.getType().isIntegerType() || param.getType().isPointerType() || param instanceof ConstFloat)
                    curMCBB.appendInst(new MCstore(
                            (Register) findContainer(param, true),
                            RealRegister.get(13),
                            new Immediate((ACTM.indexOf(crspArg)-stackSize)*4)
                    ));
                else
                    curMCBB.appendInst(new MCFPstore(
                            (ExtensionRegister) findFloatContainer(param),
                            RealRegister.get(13),
                            new Immediate((ACTM.indexOf(crspArg)-stackSize)*4)
                    ));
            }
            else if (APVER.contains(crspArg))
                curMCBB.appendInst(new MCFPmove(RealExtRegister.get(APVER.indexOf(crspArg)), findFloatContainer(param, false)));
            else if (APVCR.contains(crspArg))
                curMCBB.appendInst(new MCMove(RealRegister.get(APVCR.indexOf(crspArg)), findContainer(param)));
        }

        /* SUB sp here, instead of using PUSH, to avoid segmentation fault if spilling happens in parameter passing */
        if (stackSize > 0)
            curMCBB.appendInst(new MCBinary(MCInstruction.TYPE.SUB, RealRegister.get(13), RealRegister.get(13), createConstInt(stackSize * 4)));
        /* Branch */
        curMCBB.appendInst(new MCbranch(target.findMCFunc((Function) IRinst.getOperandAt(0))));
        /* Stack balancing */
        if (stackSize > 0)
            curMCBB.appendInst(new MCBinary(MCInstruction.TYPE.ADD, RealRegister.get(13), RealRegister.get(13), createConstInt(stackSize * 4)));
        /* Save result */
        if (callee.getType().getRetType().isFloatType())
            curMCBB.appendInst(new MCFPmove((VirtualExtRegister) findFloatContainer(IRinst), RealExtRegister.get(0)));
        else
            curMCBB.appendInst(new MCMove((Register) findContainer(IRinst), RealRegister.get(0)));

        curFunc.setUseLR();
    }

    private void translateRet(TerminatorInst.Ret IRinst) {
        if (IRinst.getNumOperands() != 0) {
            var returnValue = IRinst.getOperandAt(0);
            if (returnValue.getType().isFloatType()){
                MCOperand ret = findFloatContainer(returnValue, false);
                curMCBB.appendInst(new MCFPmove(RealExtRegister.get(0),  ret));
            }
            else
                curMCBB.appendInst(new MCMove(RealRegister.get(0), findContainer(returnValue)));
        }
        curMCBB.appendInst(new MCReturn());
    }

    /**
     * Translate the IR alloca instruction into ARM. <br/>
     * IR Alloca will be translated into "VirReg := sp + offset_before".
     * And sp will be added in front of a procedure,
     * meaning that the stack of function is allocated at beginning,
     * and do NOT change in the procedure until a function call then balanced. <br/>
     * @param IRinst IR instruction to be translated
     */
    private void translateAlloca(MemoryInst.Alloca IRinst) {
        // TODO: calculate address when used
        int offset = 0;
        Type allocated = IRinst.getAllocatedType();
        if (allocated.isIntegerType() || allocated.isFloatType() || allocated.isPointerType()) {
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
     * @param IRinst IR instruction to be translated
     */
    private void translateStore(MemoryInst.Store IRinst) {
        Value source = IRinst.getOperandAt(0);
        Register addr = ((Register) findContainer(IRinst.getOperandAt(1)));
        if (source.getType().isIntegerType() || source.getType().isPointerType() || source instanceof ConstFloat){
            Register src = ((Register) findContainer(source, true));
            curMCBB.appendInst(new MCstore(src, addr));
        }
        else {
            ExtensionRegister src = (ExtensionRegister) findFloatContainer(source);
            curMCBB.appendInst(new MCFPstore(src, addr));
        }
    }

    /**
     * Translate IR load, just like its name.
     * @param IRinst IR instruction to be translated
     */
    private void translateLoad(MemoryInst.Load IRinst) {
        if (IRinst.getType().isFloatType())
            curMCBB.appendInst(new MCFPload((ExtensionRegister) findFloatContainer(IRinst), (Register) findContainer(IRinst.getOperandAt(0))));
        else
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
                MCInstruction.ConditionField cond = dealCmpOpr((BinaryOpInst) IRinst.getOperandAt(0), false);
                curMCBB.appendInst(new MCbranch(curFunc.findMCBB((BasicBlock) IRinst.getOperandAt(1)), cond));
                curMCBB.appendInst(new MCbranch(curFunc.findMCBB((BasicBlock) IRinst.getOperandAt(2)), reverseCond(cond)));
            }
        }
        else {
            curMCBB.appendInst(new MCbranch(curMCBB.findMCBB((BasicBlock) IRinst.getOperandAt(0))));
        }
    }

    /**
     * Translate all the compare instruction <br/>
     * ZEXT will be translated to a CMP with saveResult true
     * @param cmp the conditional instruction, including ICMP, FCMP, ZEXT
     * @param saveResult whether to save the compare result to a register
     * @return the corresponding ARM condition field of the compare
     */
    private MCInstruction.ConditionField dealCmpOpr(Instruction cmp, boolean saveResult) {
        if (cmp.isIcmp())
            return translateIcmp((BinaryOpInst) cmp, saveResult);
        else if (cmp.isFcmp())
            return translateFcmp((BinaryOpInst) cmp, saveResult);
        else if (cmp.isZext())
            return dealCmpOpr((BinaryOpInst) cmp.getOperandAt(0), true);
        else
            return null;
    }

    /**
     * Translate the IR icmp into a lot of ARM calculation.
     * @param icmp IR instruction to be translated
     * @param saveResult whether to save the compare result to a register
     * @return the corresponding ARM condition field of icmp
     */
    private MCInstruction.ConditionField translateIcmp(BinaryOpInst icmp, boolean saveResult) {
        Value value1 = icmp.getOperandAt(0);
        Value value2 = icmp.getOperandAt(1);

        /* If there is cmp or zext instruction in operands */
        if (value1 instanceof Instruction)
            dealCmpOpr((Instruction) value1, true);
        if (value2 instanceof Instruction)
            dealCmpOpr((Instruction) value2, true);

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
     * Translate Fcmp into ARM VCMP
     * @param fcmp IR FCMP instruction
     * @param saveResult whether to save the compare result to a register
     * @return the corresponding ARM condition field of fcmp
     */
    private MCInstruction.ConditionField translateFcmp(BinaryOpInst fcmp, boolean saveResult) {
        Value value1 = fcmp.getOperandAt(0);
        Value value2 = fcmp.getOperandAt(1);

        /* If there is cmp or zext instruction in operands */
        if (value1 instanceof Instruction)
            dealCmpOpr((Instruction) value1, true);
        if (value2 instanceof Instruction)
            dealCmpOpr((Instruction) value2, true);

        /* Translate */
        ExtensionRegister operand1 = (ExtensionRegister) findFloatContainer(value1);
        ExtensionRegister operand2 = (ExtensionRegister) findFloatContainer(value2);
        MCInstruction.ConditionField armCond = mapToArmCond(fcmp);
        curMCBB.appendInst(new MCFPcompare(operand1, operand2));
        curMCBB.appendInst(new MCFPmove());

        /* Save result */
        if (saveResult) {
            curMCBB.appendInst(new MCMove((Register) findContainer(fcmp), createConstInt(1), null, armCond));
            curMCBB.appendInst(new MCMove((Register) findContainer(fcmp), createConstInt(0), null, reverseCond(armCond)));
        }

        return armCond;
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
        if (value1 instanceof CastInst.ZExt)
            translateIcmp((BinaryOpInst) ((CastInst.ZExt) value1).getOperandAt(0), true);
        if (value2 instanceof CastInst.ZExt)
            translateIcmp((BinaryOpInst) ((CastInst.ZExt) value2).getOperandAt(0), true);

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
                if (type != MCInstruction.TYPE.SUB) {
                    if (((ConstInt) value1).getVal() == 0)
                        curMCBB.appendInst(new MCMove(dst, operand2));
                    else
                        curMCBB.appendInst(new MCBinary(type, dst, (Register) operand2, operand1));
                }
                else
                    curMCBB.appendInst(new MCBinary(MCInstruction.TYPE.RSB, dst, (Register) operand2, operand1));
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
                MCInstruction.TYPE type = intVal<0 ?MCInstruction.TYPE.SUB : MCInstruction.TYPE.RSB;
                MCInstruction.Shift shift = new MCInstruction.Shift(MCInstruction.Shift.TYPE.LSL, log2(abs+1));
                curMCBB.appendInst(new MCBinary(type, dst, mul, mul, shift, null));
            }
            else if (isPowerOfTwo(abs - 1)) {
                MCInstruction.Shift shift = new MCInstruction.Shift(MCInstruction.Shift.TYPE.LSL, log2(abs));
                curMCBB.appendInst(new MCBinary(MCInstruction.TYPE.ADD, dst, mul, mul, shift, null));
                if (intVal < 0) {
                    curMCBB.appendInst(new MCBinary(MCInstruction.TYPE.RSB, dst, dst, createConstInt(0)));
                }
            }
            else {
                Register mul1 = (Register) findContainer(operand1, true);
                Register mul2 = (Register) findContainer(operand2, true);
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

    private void translateFloatBinary(BinaryOpInst IRinst, MCInstruction.TYPE type) {
        curMCBB.appendInst(new MCFPBinary(
                type,
                (ExtensionRegister) findFloatContainer(IRinst),
                (ExtensionRegister) findFloatContainer(IRinst.getOperandAt(0)),
                (ExtensionRegister) findFloatContainer(IRinst.getOperandAt(1))
        ));
    }

    private void translateFloatNeg(UnaryOpInst IRinst) {
        curMCBB.appendInst(new MCFPneg(
                (ExtensionRegister) findFloatContainer(IRinst),
                (ExtensionRegister) findFloatContainer(IRinst.getOperandAt(0))
        ));
    }

    private void translateConvert(CastInst IRinst, boolean f2i) {
        curMCBB.appendInst(new MCFPconvert(
                f2i,
                (ExtensionRegister) findFloatContainer(IRinst),
                (ExtensionRegister) findFloatContainer(IRinst.getOperandAt(0))
        ));
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
                    curMCBB.appendInst(new MCMove(vr, new Immediate(scale), !canEncodeImm(scale)));
                    curMCBB.appendInst(new MCFma(MCInstruction.TYPE.MLA, dst, ((Register) index), vr, baseAddr));
                }

                baseAddr = dst;
            }
        }
    }

    private void translatePhi() {
        for (BasicBlock IRBB : curIRFunc) {
            var info = blockInfo.get(IRBB);
            if (info.predecessors.size() <= 1) continue;

            var phis = new ArrayList<PhiInst>();
            for (Instruction inst : IRBB)
                if (inst instanceof PhiInst)
                    phis.add((PhiInst) inst);
                else
                    break;

            for (var pre : info.predecessors) {
                /* Build the map between phi operands */
                var preIRBB = pre.getRawBasicBlock();
                var phiMap = new HashMap<MCOperand, MCOperand>();

                phis.forEach(phi -> {
                    if (phi.getType().isFloatType())
                        phiMap.put(findFloatContainer(phi), findFloatContainer(phi.findValue(preIRBB)));
                    else // FIXME: 可能导致立即数报错
                        phiMap.put(findContainer(phi), findContainer(phi.findValue(preIRBB)));
                });

                /* Generate code */
                /* Store the moves in reversed order for insert */
                var moves = new LinkedList<MCInstruction>();

                while (!phiMap.isEmpty()) {
                    var k = phiMap.keySet().iterator().next();

                    /* Build the use stack */
                    var useStack = new Stack<MCOperand>();
                    var dealing = k;
                    while (true) {
                        if (!phiMap.containsKey(dealing))
                            break;
                        else if (useStack.contains(dealing))
                            break;
                        else {
                            useStack.push(dealing);
                            dealing = phiMap.get(dealing);
                        }
                    }

                    /* Dealing phi in a loop */
                    if (phiMap.containsKey(dealing)) {
                        VirtualRegister tmp = curFunc.createVirReg(null);
                        MCOperand dst = tmp;
                        MCOperand src;
                        while (useStack.contains(dealing)) {
                            src = useStack.pop();
                            moves.addFirst(new MCMove((Register) dst, src));
                            dst = src;
                            phiMap.remove(src);
                        }
                        moves.addFirst(new MCMove((Register) dealing, tmp));

                    }
                    /* Dealing nested phi */
                    MCOperand dst;
                    MCOperand src = dealing;
                    while (!useStack.isEmpty()) {
                        dst = useStack.pop();
                        moves.addFirst(new MCMove((Register) dst, src));
                        src = dst;
                        phiMap.remove(dst);
                    }
                }

                /* Insert */
                var preList = curFunc.findMCBB(preIRBB).getInstructionList();
                for (int i=preList.size(); i-->0; ) {
                    var inst = preList.get(i);
                    if (inst instanceof MCbranch)
                        continue;
                    else {
                        moves.forEach(inst::insertAfter);
                        break;
                    }
                }
            }
        }
    }
    //</editor-fold>

}