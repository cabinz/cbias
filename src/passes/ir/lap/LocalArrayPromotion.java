package passes.ir.lap;

import ir.Module;
import passes.ir.IRPass;

/**
 * Wrapper for LocalArrayPromotion
 */
public class LocalArrayPromotion implements IRPass {
    @Override
    public void runOnModule(Module module) {
        module.functions.forEach(function -> LocalArrayPromotionRaw.optimize(module, function));
    }

}
