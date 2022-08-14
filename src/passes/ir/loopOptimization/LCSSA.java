package passes.ir.loopOptimization;

import ir.Module;
import ir.Use;
import ir.values.BasicBlock;
import ir.values.instructions.PhiInst;
import passes.ir.IRPass;
import ir.values.Instruction;
import passes.ir.analysis.LoopAnalysis;

import java.util.*;

public class LCSSA implements IRPass {

    /* BasicBlock directly contained in loop */
    HashMap<LoopAnalysis<LoopBB>.Loop, List<LoopBB>> loopsMap;

    @Override
    public void runOnModule(Module module) {
        for (var func : module.functions) {
            HashMap<BasicBlock, LoopBB> bbmap = new HashMap<>();
            func.forEach(bb -> bbmap.put(bb, new LoopBB(bb)));
            LoopAnalysis.analysis(bbmap);

            fillLoop(bbmap.values());

            loopsMap.keySet().forEach(this::replaceOuterUse);
        }
    }

    private void fillLoop(Collection<LoopBB> bbs) {
        loopsMap = new HashMap<>();
        bbs.forEach(bb -> {
            var loop = bb.getLoop();
            if (loopsMap.containsKey(loop)) {
                loopsMap.get(loop).add(bb);
            }
            else {
                var loopBBs = new LinkedList<LoopBB>();
                loopBBs.add(bb);
                loopsMap.put(loop, loopBBs);
            }

            while (loop.getOuterLoop() != null) {
                loop = loop.getOuterLoop();
                if (loopsMap.containsKey(loop)) {
                    loopsMap.get(loop).add(bb);
                }
                else {
                    var loopBBs = new LinkedList<LoopBB>();
                    loopBBs.add(bb);
                    loopsMap.put(loop, loopBBs);
                }
            }
        });
    }

    private void replaceOuterUse(LoopAnalysis<LoopBB>.Loop loop) {
        var outerUses = getOuterUses(loop);

        for (var use : outerUses) {
            var usee = (Instruction) use.getUsee();
            var user = (Instruction) use.getUser();
            var innerBlock = usee.getBB();
            var outerBlock = user.getBB();

            var phi = new PhiInst(usee.getType());
            phi.addMapping(innerBlock, usee);

            outerBlock.insertAtFront(phi);
            user.setOperandAt(use.getOperandPos(), phi);
        }
    }

    private Set<Use> getOuterUses(LoopAnalysis.Loop loop) {
        var ret = new HashSet<Use>();
        var bbs = loopsMap.get(loop);

        for (LoopBB bb : bbs) {
            for (Instruction inst : bb.getRawBasicBlock()) {
                for (Use use : inst.getUses()) {
                    var user = use.getUser();
                    if (bbs.stream().noneMatch(loopBB -> loopBB.getRawBasicBlock().getInstructions().contains(user)))
                        ret.add(use);
                }
            }
        }

        return ret;
    }
}
