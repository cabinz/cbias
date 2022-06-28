package frontend;

import ir.Module;
import ir.Type;
import ir.Value;
import ir.types.FunctionType;
import ir.types.IntegerType;
import ir.types.PointerType;
import ir.values.*;
import ir.values.instructions.*;

import java.util.ArrayList;

/**
 * An IRBuilder object maintains one building process of the IR.
 * <br>
 * It keeps track of the current positions of module, function and basic block
 * during building an IR module.
 * <br>
 * It can be used to create different types of IR constructs and insert constructs
 * Technically, Visitor should only use methods provided by IRBuilder to build IR
 * (e.g. build constructs, retrieve Constants), other than directly invoking
 * constructors of Values, which might lead to unsafe behaviors and chaos code.
 * <br>
 */
public class IRBuilder {

    //<editor-fold desc="Fields">

    //<editor-fold desc="Three current building pointers for different levels of IR.">
    /**
     * The module currently being built.
     * In our case with only a single compile unit to work on,
     * this reference is in effect immutable.
     */
    private Module curMdl;

    /**
     * The function currently being built.
     */
    private Function curFunc;

    /**
     * The basic block currently being built.
     */
    private BasicBlock curBB;
    //</editor-fold>

    //</editor-fold>


    //<editor-fold desc="Constructors">
    /**
     * @param m The module to build on.
     */
    public IRBuilder(Module m) {
        this.setCurModule(m);
    }
    //</editor-fold>


    //<editor-fold desc="Methods">

    //<editor-fold desc="Setters for building pointers.">
    public void setCurModule(Module mod) {
        curMdl = mod;
    }

    public void setCurFunc(Function func) {
        curFunc = func;
    }

    public void setCurBB(BasicBlock bb) {
        curBB = bb;
    }
    //</editor-fold>

    //<editor-fold desc="Getters for building pointers.">
    public Module getCurModule() {
        return curMdl;
    }

    public Function getCurFunc() {
        return curFunc;
    }

    public BasicBlock getCurBB() {
        return curBB;
    }
    //</editor-fold>

    /**
     * Retrieve a Constant.ConstInt Value.
     * @param i The numeric value of the integer.
     * @return The ConstInt Value with given numeric value.
     */
    public Constant.ConstInt buildConstant(int i) {
        return Constant.ConstInt.get(i);
    }

    /**
     * Retrieve a Constant.ConstArray Value.
     * @param arrType The ArrayType carrying necessary info.
     * @param arr An array of (integer/float) Constants for initialization the array.
     * @return The ConstArray.
     */
    public Constant.ConstArray buildConstArr(Type arrType, ArrayList<Constant> arr) {
        return new Constant.ConstArray(arrType, arr);
    }

    /**
     * Insert a function into the module, with function name assigned.
     * Argument list and return type parts will leave to the child node
     * visiting.
     * @param name Function name (identifier).
     * @param type Value type.
     * @return Reference of the function created and inserted.
     */
    public Function buildFunction(String name, FunctionType type, boolean isExternal) {
        Function func = new Function(type, isExternal);
        func.setName(name);
        // Add the function to the current module.
        if(isExternal) {
            getCurModule().externFunctions.add(func);
        }
        else {
            getCurModule().functions.add(func);
        }
        // Set the pointer.
        this.setCurFunc(func);
        return func;
    }

    /**
     * Insert a new basic block at the end of current function.
     * @param bbName Name of the basic block to be created.
     * @return Reference of the basic block created.
     */
    public BasicBlock buildBB(String bbName) {
        BasicBlock bb = new BasicBlock(bbName);
        curFunc.addBB(bb);
        // Set the pointer.
        this.setCurBB(bb);
        return bb;
    }

    /**
     * Insert a Ret terminator with no return value (return void)
     * at the end of current basic block.
     * @return The terminator object inserted.
     */
    public TerminatorInst.Ret buildRet() {
        TerminatorInst.Ret ret = new TerminatorInst.Ret(curBB);
        getCurBB().insertAtEnd(ret);
        return ret;
    }

    /**
     * Insert a Ret terminator with a specified return value
     * at the end of current basic block.
     * @param retVal The Value instance specified as return value.
     * @return The terminator object inserted.
     */
    public TerminatorInst.Ret buildRet(Value retVal) {
        TerminatorInst.Ret ret = new TerminatorInst.Ret(retVal, curBB);
        getCurBB().insertAtEnd(ret);
        return ret;
    }

    /**
     * Insert a conditional branching instruction at the end of current basic block.
     * @param cond The condition.
     * @param trueBlk The basic block to jump to when condition is true.
     * @param falseBlk The basic block to jump to when condition is false.
     * @return The conditional branching inserted.
     */
    public TerminatorInst.Br buildBr(Value cond, BasicBlock trueBlk, BasicBlock falseBlk) {
        // Create and insert a block.
        TerminatorInst.Br condBr = new TerminatorInst.Br(cond, trueBlk, falseBlk, curBB);
        getCurBB().insertAtEnd(condBr);
        // Build the directed connections among blocks.
        getCurBB().addSuccessor(trueBlk);
        getCurBB().addSuccessor(falseBlk);
        trueBlk.addPredecessor(getCurBB());
        falseBlk.addPredecessor(getCurBB());

        return condBr;
    }

    /**
     * Insert an unconditional branching instruction at the end of current basic block.
     * @param blk The basic block to jump to.
     * @return The unconditional branching inserted.
     */
    public TerminatorInst.Br buildBr(BasicBlock blk) {
        // Create and insert a block.
        TerminatorInst.Br uncondBr = new TerminatorInst.Br(blk, curBB);
        getCurBB().insertAtEnd(uncondBr);
        // Build the directed connections among blocks.
        getCurBB().addSuccessor(blk);
        blk.addPredecessor(getCurBB());

        return uncondBr;
    }

    /**
     * Insert a Call terminator invoking a given function with given list of arguments
     * at the end of current basic block.
     * The new basic block will not be automatically created by this method and
     * needs to be manually built and managed by the user.
     * @param func Function Value carrying information about return type and FORMAL arguments.
     * @param args The ACTUAL arguments to be referenced by the Call.
     * @return The call instruction inserted.
     */
    public CallInst buildCall(Function func, ArrayList<Value> args) {
        CallInst call = new CallInst(func, args, curBB);
        getCurBB().insertAtEnd(call);
        return call;
    }

    /**
     * Insert a Load instruction at the end of current basic block.
     * @param loadedType The type of the memory block loaded in.
     * @param addr loadedType*
     * @return The Load instruction inserted.
     */
    public MemoryInst.Load buildLoad(Type loadedType, Value addr) {
        MemoryInst.Load inst = new MemoryInst.Load(loadedType, addr, curBB);
        getCurBB().insertAtEnd(inst);
        return inst;
    }

    /**
     * Insert a Store instruction at the end of current basic block.
     * @param val The Value to be stored (written) back to memory.
     * @param addr The address where the content to be written.
     * @return The Store instruction inserted.
     */
    public MemoryInst.Store buildStore(Value val, Value addr) {
        MemoryInst.Store inst = new MemoryInst.Store(val, addr, curBB);
        getCurBB().insertAtEnd(inst);
        return inst;
    }

    /**
     * Insert a Alloca instruction at the FRONT of current basic block.
     * @param allocatedType The type of memory space allocated.
     * @return The Alloca instruction inserted.
     */
    public MemoryInst.Alloca buildAlloca(Type allocatedType) {
        MemoryInst.Alloca inst = new MemoryInst.Alloca(allocatedType, curBB);
        getCurBB().insertAtFront(inst);
        return inst;
    }

    /**
     * Insert a ZExt instruction at current position of basic block.
     * @param srcVal The Value to be extended.
     * @return The ZExt instruction inserted.
     */
    public MemoryInst.ZExt buildZExt(Value srcVal) {
        MemoryInst.ZExt zext = new MemoryInst.ZExt(srcVal, curBB);
        getCurBB().insertAtEnd(zext);
        return zext;
    }

    /**
     * Insert a binary instruction at current position of basic block.
     * @param tag Instruction category.
     * @param lOp Left operand.
     * @param rOp Right operand.
     * @return The binary instruction inserted.
     */
    public BinaryInst buildBinary(Instruction.InstCategory tag, Value lOp, Value rOp) {
        // Analyze the type of the result returned by the binary operation.
        Type resType = null;
        if (tag.isRelationalBinary()) {
            resType = IntegerType.getI1();
        }
        else if (tag.isArithmeticBinary()) {
            resType = IntegerType.getI32();
        }
        else {
            // todo: float arithmetic binary operations
        }
        // Build the binary instruction.
        BinaryInst binInst = new BinaryInst(resType, tag, lOp, rOp, curBB);
        getCurBB().insertAtEnd(binInst);
        return binInst;
    }

    /**
     * Build a GlbVar w/o initialization.
     * @param name The name of the GlbVar.
     * @param type The type of the memory block it references.
     */
    public GlobalVariable buildGlbVar(String name, Type type) {
        GlobalVariable glbVar = new GlobalVariable(name, type);
        getCurModule().addGlbVar(glbVar);
        return glbVar;
    }

    /**
     * Build a GlbVar with initialization.
     * @param name The name of the GlbVar.
     * @param init The initial value.
     */
    public GlobalVariable buildGlbVar(String name, Constant init) {
        GlobalVariable glbVar = new GlobalVariable(name, init);
        getCurModule().addGlbVar(glbVar);
        return glbVar;
    }


    /**
     * Insert a GEP instruction at current position of basic block.
     * @param ptr The Value in PointerType (the first address of an array).
     * @param indices The indices for dereference.
     * @return The GEP instruction inserted.
     */
    public GetElemPtrInst buildGEP(Value ptr, ArrayList<Value> indices) {
        GetElemPtrInst gepInst = new GetElemPtrInst(ptr, indices, curBB);
        getCurBB().insertAtEnd(gepInst);
        return gepInst;
    }
    //</editor-fold>
}
