package passes;

import ir.Module;
import passes.mem2reg.Mem2reg;

public class PassManager {

    /**
     * Running through passes to optimize a module.
     *
     * @param module The module to be optimized.
     */
    public static void runPasses(Module module){
        Mem2reg.optimize(module);
    }
}
