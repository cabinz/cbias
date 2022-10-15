package passes.ir.gcm;

import frontend.IREmitter;
import ir.Module;
import passes.PassManager;
import passes.ir.IRPass;
import passes.ir.dce.UnreachableCodeElim;
import passes.ir.dce.UselessCodeElim;

import java.io.IOException;

/**
 * <p>Wrapper for global code motion.</p>
 */
public class GlobalCodeMotion implements IRPass {

    @Override
    public void runOnModule(Module module) {
        // Do DCE first to accelerate optimize.
        PassManager.getInstance().run(UselessCodeElim.class, module);
        PassManager.getInstance().run(UnreachableCodeElim.class, module);
        GlobalCodeMotionRaw.optimize(module);
    }

}
