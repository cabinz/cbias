package passes.ir;

import ir.Module;

/**
 * Interface for all IR passes.
 */
public interface IRPass {
    void runOnModule(Module module);
}
