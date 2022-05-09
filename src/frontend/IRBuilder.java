package frontend;

import ir.Module;
import ir.types.FunctionType;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.instructions.TerminatorInst;

/**
 * An IRBuilder object maintains one building process of the IR.
 * It keeps track of the module, function and basic block that currently building.
 * It has methods for creating different types of IR constructs, encapsulating type
 * casting, assignments and all the complicated works needed for initialize them,
 * as well as methods for inserting these constructions created into the IR.
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
     * If no existed module given, a new module object will be initialized
     * automatically.
     */
    public IRBuilder() {
        curMdl = new Module();
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
     * Insert a Ret terminator at the end of current function.
     * @return The terminator object inserted.
     */
    public TerminatorInst.Ret buildRet() {
        TerminatorInst.Ret ret = new TerminatorInst.Ret();
        getCurBB().instructions.add(ret);
        return ret;
    }

    //</editor-fold>
}
