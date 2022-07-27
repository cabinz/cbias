package passes.mc.registerAllocation;

import backend.ARMAssemble;
import passes.mc.MCPass;

public class RegisterAllocation implements MCPass {

    @Override
    public void runOnModule(ARMAssemble armAssemble) {

        var extRegAlloc = new GC4VER();
        extRegAlloc.runOnModule(armAssemble);

        var coreRegAlloc = new GraphColoring();
        coreRegAlloc.runOnModule(armAssemble);
    }
}
