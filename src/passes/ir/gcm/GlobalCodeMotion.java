package passes.ir.gcm;

import frontend.IREmitter;
import ir.Module;
import passes.PassManager;
import passes.ir.IRPass;
import passes.ir.dce.UnreachableCodeElim;
import passes.ir.dce.UselessCodeElim;

import java.io.IOException;

public class GlobalCodeMotion implements IRPass {

    @Override
    public void runOnModule(Module module) {
        PassManager.getInstance().run(UselessCodeElim.class, module);
        PassManager.getInstance().run(UnreachableCodeElim.class, module);
        try {
            (new IREmitter("debug.ll")).emit(module);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        GlobalCodeMotionRaw.optimize(module);
    }

}
