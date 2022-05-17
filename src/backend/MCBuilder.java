package backend;

import backend.armCode.MCBasicBlock;
import backend.armCode.MCFunction;
import backend.armCode.MCInstruction;
import backend.armCode.MCmov;
import backend.operand.Immediate;
import backend.operand.RealRegister;
import ir.Module;
import ir.values.BasicBlock;
import ir.values.Constant;
import ir.values.Function;
import ir.values.Instruction;

/**
 * This class is used to CodeGen, or translate LLVM IR into ARM assemble
 * (both are in memory). As the emitter, it's a Singleton.
 */
public class MCBuilder {

    //<editor-fold desc="Singleton Pattern">
    static private MCBuilder builder = new MCBuilder();

    private MCBuilder() {};

    static public MCBuilder get() {return builder;}
    //</editor-fold>

    private Module IRModule;


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
        if (IRinst.isRet()){
            return new MCmov(MCBB, new RealRegister(RealRegister.NAME.r0), new Immediate( ((Constant.ConstInt)(IRinst.getOperandAt(0))).getVal() ));
        }
        else return null;
    }
}
