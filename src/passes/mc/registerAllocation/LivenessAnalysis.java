package passes.mc.registerAllocation;

import backend.armCode.MCBasicBlock;
import backend.armCode.MCFPInstruction;
import backend.armCode.MCFunction;
import backend.armCode.MCInstruction;
import backend.armCode.MCInstructions.MCbranch;
import backend.operand.ExtensionRegister;
import backend.operand.MCOperand;
import backend.operand.Register;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Liveness analyze
 */
public class LivenessAnalysis {

    public static HashMap<MCOperand, Integer> liveRangeAnalysis(MCFunction func) {
        var liveRange = new HashMap<MCOperand, Integer>();
        var first = new HashMap<MCOperand, Integer>();
        var last = new HashMap<MCOperand, Integer>();
        int pc = 0;

        var q = new LinkedList<MCBasicBlock>();
        var visited = new HashSet<MCBasicBlock>();
        q.addLast(func.getEntryBlock());

        /* BFS */
        while (!q.isEmpty()) {
            var dealing = q.getFirst();
            q.removeFirst();
            visited.add(dealing);

            var next = dealing.getTrueSuccessor();
            if (next != null && !visited.contains(next)) {
                q.addLast(next);
                visited.add(next);
            }
            next = dealing.getFalseSuccessor();
            if (next != null && !visited.contains(next)) {
                q.addLast(next);
                visited.add(next);
            }

            for (MCInstruction inst : dealing) {
                pc++;
                for (var def : inst.getDef()) {
                    if (!first.containsKey(def)) {
                        first.put(def, pc);
                    }
                }
                for (var use : inst.getUse()) {
                    last.put(use, pc);
                }

                if (inst instanceof MCFPInstruction) {
                    var vfpInst = ((MCFPInstruction) inst);
                    for (var def : vfpInst.getExtDef()) {
                        if (!first.containsKey(def)) {
                            first.put(def, pc);
                        }
                    }
                    for (var use : vfpInst.getExtUse()) {
                        last.put(use, pc);
                    }
                }
            }
        }

        for (var x : last.keySet()) {
            liveRange.put(x, last.getOrDefault(x, 0)-first.getOrDefault(x, pc));
        }

        return liveRange;
    }

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
                if (inst instanceof MCFPInstruction) {
                    var fpInst = (MCFPInstruction) inst;
                    fpInst.getExtUse().stream()
                            .filter(extUse -> !info.extDef.contains(extUse))
                            .forEach(info.extUse::add);
                    fpInst.getExtDef().stream()
                            .filter(extDef -> !info.extUse.contains(extDef))
                            .forEach(info.extDef::add);
                }
                else if (inst instanceof MCbranch) {
                    var br = (MCbranch) inst;
                    br.getExtUse().stream()
                            .filter(extUse -> !info.extDef.contains(extUse))
                            .forEach(info.extUse::add);
                    br.getExtDef().stream()
                            .filter(extDef -> !info.extUse.contains(extDef))
                            .forEach(info.extDef::add);

                }
            }

            /* Add all use into in */
            info.in.addAll(info.use);
            info.extIn.addAll(info.extUse);
        }

        /* Calculate out set until convergence */
        boolean convergence = false;
        while (!convergence) {
            convergence = true;
            /* Calculate each block */
            for (MCBasicBlock block : func) {
                var info = liveMap.get(block);
                var out = new HashSet<Register>();
                var extOut = new HashSet<ExtensionRegister>();

                /* out = union(in_of_all_successor) */
                if (block.getTrueSuccessor() != null) {
                    out.addAll(liveMap.get(block.getTrueSuccessor()).in);
                    extOut.addAll(liveMap.get(block.getTrueSuccessor()).extIn);
                }
                if (block.getFalseSuccessor() != null) {
                    out.addAll(liveMap.get(block.getFalseSuccessor()).in);
                    extOut.addAll(liveMap.get(block.getFalseSuccessor()).extIn);
                }

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
                if (!extOut.equals(info.extOut)) {
                    convergence = false;
                    /* Set out = new out */
                    info.extOut = extOut;
                    /* Set in = union(use, out\def) */
                    info.extIn  = new HashSet<>(info.extUse);
                    info.extOut.stream()
                            .filter(x -> !info.extDef.contains(x))
                            .forEach(info.extIn::add);
                }
            }
        }

        return liveMap;
    }
}