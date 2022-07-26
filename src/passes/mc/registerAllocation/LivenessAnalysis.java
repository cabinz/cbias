package passes.mc.registerAllocation;

import backend.armCode.MCBasicBlock;
import backend.armCode.MCFunction;
import backend.armCode.MCInstruction;
import backend.operand.Register;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Liveness analyze
 */
public class LivenessAnalysis {

    public static HashMap<MCBasicBlock, LiveInfo> run(MCFunction func){
        var liveMap = new HashMap<MCBasicBlock, LiveInfo>();

        /* Initialize use, def & in set */
        for (MCBasicBlock block : func) {
            /* Initialize */
            LiveInfo info = new LiveInfo(block);
            liveMap.put(block, info);

            /* Build use & def set */
            for (MCInstruction inst : block) {
                inst.getUse().stream()
                        .filter(use -> !info.def.contains(use))
                        .forEach(info.use::add);
                inst.getDef().stream()
                        .filter(def -> !info.use.contains(def))
                        .forEach(info.def::add);
            }

            /* Add all use into in */
            info.in.addAll(info.use);
        }

        /* Calculate out set until convergence */
        boolean convergence = false;
        while (!convergence) {
            convergence = true;
            /* Calculate each block */
            for (MCBasicBlock block : func) {
                var info = liveMap.get(block);
                var out = new HashSet<Register>();

                /* out = union(in_of_all_successor) */
                if (block.getTrueSuccessor() != null)
                    out.addAll(liveMap.get(block.getTrueSuccessor()).in);
                if (block.getFalseSuccessor() != null)
                    out.addAll(liveMap.get(block.getFalseSuccessor()).in);

                if (!out.equals(info.out)) {
                    convergence = false;
                    /* Set out = new out */
                    info.out = out;
                    /* Set in = union(use, out\def) */
                    info.in  = new HashSet<>(info.use);
                    info.out.stream()
                            .filter(x -> !info.def.contains(x))
                            .forEach(info.in::add);
                }
            }
        }

        return liveMap;
    }
}
