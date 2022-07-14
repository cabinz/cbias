package passes.mc.RegisterAllocation;

import backend.armCode.MCBasicBlock;
import backend.armCode.MCFunction;

import java.util.HashMap;

/**
 * Liveness analyze
 */
public class LivenessAnalysis {

    public static HashMap<MCBasicBlock, LiveInfo> run(MCFunction func){
        return new HashMap<>();
    }
}
