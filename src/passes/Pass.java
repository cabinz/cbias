package passes;

import ir.Module;

public interface Pass {
    void runOnModule(Module module);
}
