package passes.mc;

import backend.ARMAssemble;

public interface MCPass {
    void runOnModule(ARMAssemble armAssemble);
}
