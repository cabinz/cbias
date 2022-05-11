package frontend;

import ir.Module;
import ir.Type;
import ir.Value;
import ir.types.FunctionType;
import ir.values.BasicBlock;
import ir.values.Constant;
import ir.values.Function;
import ir.values.Instruction;
import ir.values.instructions.BinaryInst;
import ir.values.instructions.MemoryInst;
import ir.values.instructions.TerminatorInst;

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
    /*
     Three current building pointers on different levels of the IR.
     */
    private Module curMdl;
    private Function curFunc;
    private BasicBlock curBB;
    //</editor-fold>


    //<editor-fold desc="Constructors">
    /**
     * @param m The module to build on.
     */
    public IRBuilder(Module m) {
        curMdl = m;
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
     * Insert a function into the module, with function name assigned.
     * Argument list and return type parts will leave to the child node
     * visiting.
     * @param name Function name (identifier).
     * @param type Value type.
     * @return Reference of the function created and inserted.
     */
    public Function buildFunction(String name, FunctionType type) {
        Function func = new Function(type);
        func.name = name;
        // Set the pointer.
        curFunc = func;
        return func;
    }

    /**
     * Insert a new basic block at the end of current function.
     * @param bbName Name of the basic block to be created.
     * @return Reference of the basic block created.
     */
    public BasicBlock buildBB(String bbName) {
        BasicBlock bb = new BasicBlock(bbName);
        curFunc.bbs.add(bb);
        // Set the pointer.
        curBB = bb;
        return bb;
    }

    /**
     * Insert a Ret terminator with no return value (return void)
     * at the end of current basic block.
     * @return The terminator object inserted.
     */
    public TerminatorInst.Ret buildRet() {
        TerminatorInst.Ret ret = new TerminatorInst.Ret();
        getCurBB().instructions.add(ret);
        return ret;
    }

    /**
     * Insert a Ret terminator with a specified return value
     * at the end of current basic block.
     * @param retVal The Value instance specified as return value.
     * @return The terminator object inserted.
     */
    public TerminatorInst.Ret buildRet(Value retVal) {
        TerminatorInst.Ret ret = new TerminatorInst.Ret(retVal);
        getCurBB().instructions.add(ret);
        return ret;
    }

    /**
     * Insert a Load instruction at the end of current basic block.
     * @param loadedType The type of the memory block loaded in.
     * @param addr loadedType*
     * @return The Load instruction inserted.
     */
    public MemoryInst.Load buildLoad(Type loadedType, Value addr) {
        MemoryInst.Load inst = new MemoryInst.Load(loadedType, addr);
        getCurBB().instructions.add(inst);
        return inst;
    }

    /**
     * Insert a Store instruction at the end of current basic block.
     * @param val The Value to be stored (written) back to memory.
     * @param addr The address where the content to be written.
     * @return The Store instruction inserted.
     */
    public MemoryInst.Store buildStore(Value val, Value addr) {
        MemoryInst.Store inst = new MemoryInst.Store(val, addr);
        getCurBB().instructions.add(inst);
        return inst;
    }

    /**
     * Insert a Alloca instruction at the end of current basic block.
     * @param allocatedType The type of memory space allocated.
     * @return The Alloca instruction inserted.
     */
    public MemoryInst.Alloca buildAlloca(Type allocatedType) {
        MemoryInst.Alloca inst = new MemoryInst.Alloca(allocatedType);
        getCurBB().instructions.add(inst);
        return inst;
    }

    /**
     * Insert a ZExt instruction at current position of basic block.
     * @param srcVal The Value to be extended.
     * @return The ZExt instruction inserted.
     */
    public MemoryInst.ZExt buildZExt(Value srcVal) {
        MemoryInst.ZExt zext = new MemoryInst.ZExt(srcVal);
        getCurBB().instructions.add(zext);
        return zext;
    }

    /**
     * Insert a binary instruction at current position of basic block.
     * @param tag Instruction category.
     * @param lOp Left operand.
     * @param rOp Right operand.
     * @return
     */
    public BinaryInst buildBinary(Instruction.InstCategory tag, Value lOp, Value rOp) {
        // todo: analyze result type when introducing float type
        BinaryInst binInst = new BinaryInst(lOp.type, tag, lOp, rOp);
        getCurBB().instructions.add(binInst);
        return binInst;
    }

    //</editor-fold>
}
