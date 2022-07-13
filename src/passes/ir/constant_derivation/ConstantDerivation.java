package passes.ir.constant_derivation;

import ir.Module;
import ir.Use;
import ir.values.GlobalVariable;
import ir.values.instructions.MemoryInst;
import passes.ir.IRPass;

import java.util.List;

/**
 * This optimization includes the following passes: <br />
 * 1) Derive global constant.
 * 2) Derive const instruction.
 * 3) Derive const branch condition.
 */
public class ConstantDerivation implements IRPass {

    @Override
    public void runOnModule(Module module) {
        optimize(module);
    }

    static void optimize(Module module){
        deriveGlobalConstant(module);
    }

    /// Derive global constant.

    static boolean isGlobalConstant(GlobalVariable variable){
        for(Use use: variable.getUses()){
            if(!(use.getUser() instanceof MemoryInst.Load)){
                return false;
            }
        }
        return true;
    }

    static void deriveGlobalConstant(Module module){
        @SuppressWarnings("unchecked")
        var globalVariableList = (List<GlobalVariable>) module.globalVariables.clone();
        globalVariableList.forEach(globalVariable -> {
            if(isGlobalConstant(globalVariable)){
                var constant = globalVariable.getInitVal();
                globalVariable.getUses().forEach(loadUse -> {
                    var loadInst = (MemoryInst.Load) loadUse.getUser();
                    @SuppressWarnings("unchecked")
                    var uses = (List<Use>) loadInst.getUses().clone();
                    uses.forEach(use -> {
                        use.setValue(constant);
                    });
                    loadInst.removeSelf();
                });
                module.globalVariables.remove(globalVariable);
            }
        });
    }

}
