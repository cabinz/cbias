package passes.ir.gcm;

import ir.Module;
import passes.PassManager;
import passes.ir.IRPass;
import passes.ir.dce.UnreachableCodeElim;
import passes.ir.dce.UselessCodeElim;

public class GlobalCodeMotion implements IRPass {

    @Override
    public void runOnModule(Module module) {
        PassManager.getInstance().run(UselessCodeElim.class, module);
        PassManager.getInstance().run(UnreachableCodeElim.class, module);
        GlobalCodeMotionRaw.optimize(module);
    }

}
