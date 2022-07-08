package passes;

import ir.Module;
import passes.mem2reg.Mem2reg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PassManager {

    public Map<Class<?>, Pass> registeredPasses = new HashMap<>();

    public PassManager(){
        ArrayList<Pass> passes = new ArrayList<>();
        passes.add(new Mem2reg());
        registerPasses(passes);
    }

    /**
     * Running through passes to optimize a module.
     *
     * @param module The module to be optimized.
     */
    public void runPasses(Module module){
        run(Mem2reg.class, module);
    }

    private void run(Class<?> passClass, Module module){
        if(registeredPasses.containsKey(passClass)){
            registeredPasses.get(passClass).runOnModule(module);
        }
    }

    public void registerPass(Pass pass){
        registeredPasses.put(pass.getClass(), pass);
    }

    public void registerPasses(Collection<Pass> passes){
        passes.forEach(this::registerPass);
    }

}
