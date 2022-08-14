package passes.ir.loopOptimization;

import passes.ir.analysis.LoopAnalysis;

import java.util.*;

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

    public static void fillLoopInfo(LoopAnalysis<LoopBB>.Loop loop) {
        for (var bb : loop.getBBs()) {
            var exits = bb.getExitBlocks().stream();
            if (exits.anyMatch(exit -> !loop.contains(exit))) {
                loop.addExiting(bb);
            }
            if (exits.anyMatch(exit -> exit==loop.getLoopHead())) {
                loop.addLatch(bb);
            }
        }
    }
}
