package passes.ir.inline;

import ir.Module;
import passes.ir.IRPass;

public class FunctionInline implements IRPass {
    @Override
    public void runOnModule(Module module) {
        (new FunctionInlineRaw()).optimize(module);
    }

}
