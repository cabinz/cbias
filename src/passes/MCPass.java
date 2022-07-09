package passes;

import backend.ARMAssemble;

public interface MCPass {
    void runOnModule(ARMAssemble armAssemble);
}
