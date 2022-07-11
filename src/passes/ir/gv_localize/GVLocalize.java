package passes.ir.gv_localize;

import ir.Module;
import passes.ir.IRPass;

public class GVLocalize implements IRPass {

    @Override
    public void runOnModule(Module module) {
        GVLocalize.optimize(module);
    }

    public static void optimize(Module module){

    }

}
