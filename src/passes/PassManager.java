package passes;

import backend.ARMAssemble;
import ir.Module;
import passes.ir.IRPass;
import passes.ir.gv_localize.GVLocalize;
import passes.mc.MCPass;
import passes.ir.mem2reg.Mem2reg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PassManager {

    private final Map<Class<?>, IRPass> registeredIRPasses = new HashMap<>();
    private final Map<Class<?>, MCPass> registeredMCPasses = new HashMap<>();

    // run

    /**
     * Running through passes to optimize a module.
     *
     * @param module The module to be optimized.
     */
    public void runPasses(Module module){
        run(GVLocalize.class, module);
        run(Mem2reg.class, module);
    }

    /**
     * Running through passes to optimize an ARMAssemble.
     *
     * @param module The module to be optimized.
     */
    public void runPasses(ARMAssemble module){

    }

    private void run(Class<?> passClass, Module module){
        if(registeredIRPasses.containsKey(passClass)){
            registeredIRPasses.get(passClass).runOnModule(module);
        }
    }

    private void run(Class<?> passClass, ARMAssemble module){
        if(registeredMCPasses.containsKey(passClass)){
            registeredMCPasses.get(passClass).runOnModule(module);
        }
    }

    // register pass

    public static void registerPass(IRPass irPass){
        getInstance().registeredIRPasses.put(irPass.getClass(), irPass);
    }

    public static void registerPass(MCPass mcPass){
        getInstance().registeredMCPasses.put(mcPass.getClass(), mcPass);
    }

    public static void registerIRPasses(Collection<IRPass> IRPasses){
        IRPasses.forEach(PassManager::registerPass);
    }

    public static void registerMCPasses(Collection<MCPass> IRPasses){
        IRPasses.forEach(PassManager::registerPass);
    }

    // singleton

    private static PassManager instance = null;

    public static PassManager getInstance(){
        if(instance==null){
            instance = new PassManager();
            // IR Passes
            ArrayList<IRPass> IRPasses = new ArrayList<>();
            IRPasses.add(new Mem2reg());
            IRPasses.add(new GVLocalize());
            registerIRPasses(IRPasses);

            // MC Passes
        }
        return instance;
    }

}
