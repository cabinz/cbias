package passes;

import backend.ARMAssemble;
import ir.Module;
import passes.ir.IRPass;
import passes.ir.constant_derivation.ConstantDerivation;
import passes.ir.dce.UnreachableCodeElim;
import passes.ir.dce.UselessCodeElim;
import passes.ir.gcm.GlobalCodeMotion;
import passes.ir.inline.FunctionInline;
import passes.ir.lap.LocalArrayPromotion;
import passes.ir.mem2reg.Mem2reg;
import passes.ir.simplify.AddInstMerge;
import passes.ir.simplify.BlockMerge;
import passes.ir.simplify.LoadStoreMerge;
import passes.mc.MCPass;
import passes.mc.buildCFG.BuildCFG;
import passes.mc.mergeBlock.MergeBlock;
import passes.mc.peepHole.PeepHole;
import passes.mc.registerAllocation.RegisterAllocation;

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
    public void runPasses(Module module) {
        // Basic optimizations
        run(Mem2reg.class, module);
        basicOptimize(module);

        // Local array promotion
        run(LocalArrayPromotion.class, module);
        basicOptimize(module);

        // Inline functions
        run(FunctionInline.class, module);
        basicOptimize(module);

        // Merge Add Instructions
        run(AddInstMerge.class, module);
        basicOptimize(module);
    }

    public void basicOptimize(Module module) {
        run(ConstantDerivation.class, module);
        run(GlobalCodeMotion.class, module);
        run(BlockMerge.class, module);
        run(LoadStoreMerge.class, module);
    }

    /**
     * Running through passes to optimize an ARMAssemble.
     *
     * @param module The module to be optimized.
     */
    public void runPasses(ARMAssemble module) {
        run(BuildCFG.class, module);
        run(RegisterAllocation.class, module);
        run(PeepHole.class, module);
        run(MergeBlock.class, module);
    }

    public void run(Class<?> passClass, Module module) {
        if (registeredIRPasses.containsKey(passClass)) {
            registeredIRPasses.get(passClass).runOnModule(module);
        }
    }

    public void run(Class<?> passClass, ARMAssemble module) {
        if (registeredMCPasses.containsKey(passClass)) {
            registeredMCPasses.get(passClass).runOnModule(module);
        }
    }

    // register pass

    public static void registerPass(IRPass irPass) {
        getInstance().registeredIRPasses.put(irPass.getClass(), irPass);
    }

    public static void registerPass(MCPass mcPass) {
        getInstance().registeredMCPasses.put(mcPass.getClass(), mcPass);
    }

    public static void registerIRPasses(Collection<IRPass> IRPasses) {
        IRPasses.forEach(PassManager::registerPass);
    }

    public static void registerMCPasses(Collection<MCPass> IRPasses) {
        IRPasses.forEach(PassManager::registerPass);
    }

    // singleton

    private static PassManager instance = null;

    public static PassManager getInstance() {
        if (instance == null) {
            instance = new PassManager();
            // IR Passes
            ArrayList<IRPass> IRPasses = new ArrayList<>();
            IRPasses.add(new Mem2reg());
            IRPasses.add(new ConstantDerivation());
            IRPasses.add(new UnreachableCodeElim());
            IRPasses.add(new UselessCodeElim());
            IRPasses.add(new GlobalCodeMotion());
            IRPasses.add(new BlockMerge());
            IRPasses.add(new FunctionInline());
            IRPasses.add(new LoadStoreMerge());
            IRPasses.add(new AddInstMerge());
            IRPasses.add(new LocalArrayPromotion());
            registerIRPasses(IRPasses);

            // MC Passes
            ArrayList<MCPass> MCPasses = new ArrayList<>();
            MCPasses.add(new BuildCFG());
            MCPasses.add(new RegisterAllocation());
            MCPasses.add(new MergeBlock());
            MCPasses.add(new PeepHole());
            registerMCPasses(MCPasses);
        }
        return instance;
    }

}
