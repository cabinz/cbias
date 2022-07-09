package passes;

import ir.Module;

public interface IRPass {
    void runOnModule(Module module);
}
