package passes.ir.loopOptimization;

import passes.ir.analysis.LoopAnalysis;

import java.util.*;
import java.util.stream.Collectors;

public class LoopInfo {

    public static Set<LoopAnalysis<LoopBB>.Loop> genLoopsWithBBs(Collection<LoopBB> bbs) {
        var loops = new HashSet<LoopAnalysis<LoopBB>.Loop>();
        for (var bb : bbs) {
            var loop = bb.getLoop();
            if (loop == null) continue;

            loop.addBBs(bb);
            loops.add(loop);
        }
        return loops;
    }

    public static Set<LoopAnalysis<LoopBB>.Loop> genTopLoop(Set<LoopAnalysis<LoopBB>.Loop> loops) {
        return loops.stream().filter(loop -> loop.getDepth()==1).collect(Collectors.toSet());
    }
}
