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

    HashMap<BasicBlock, LoopBB> bbmap;

    @Override
    public void runOnModule(Module module) {
        for (var func : module.functions) {
            bbmap = new HashMap<>();
            func.forEach(bb -> bbmap.put(bb, new LoopBB(bb)));
            LoopAnalysis.analysis(bbmap);

            var loops = LoopInfo.genLoopsWithBBs(bbmap.values());

            loops.forEach(this::replaceOuterUse);
        }
    }

    private void replaceOuterUse(LoopAnalysis<LoopBB>.Loop loop) {
        loop.fillLoopInfo();

        var outerUses = getOuterUses(loop);

        for (var use : outerUses) {
            var usee = (Instruction) use.getUsee();
            var user = (Instruction) use.getUser();

            var exit = loop.getHeaderExit();

            var phi = new PhiInst(usee.getType());
            for (var bb : exit.getEntryBlocks()) {
                phi.addMapping(bb.getRawBasicBlock(), usee);
            }

            exit.getRawBasicBlock().insertAtFront(phi);
            user.setOperandAt(use.getOperandPos(), phi);
        }
    }

    private Set<Use> getOuterUses(LoopAnalysis<LoopBB>.Loop loop) {
        var ret = new HashSet<Use>();
        List<LoopBB> bbs = loop.getBBs();

        for (LoopBB bb : bbs) {
            for (Instruction inst : bb.getRawBasicBlock()) {
                for (Use use : inst.getUses()) {
                    var user = (Instruction) use.getUser();
                    if (!loop.contains(bbmap.get(user.getBB())))
                        ret.add(use);
                }
            }
        }

        return ret;
    }
}