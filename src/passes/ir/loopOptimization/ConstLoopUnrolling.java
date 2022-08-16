package passes.ir.loopOptimization;

import ir.Module;
import ir.Value;
import ir.values.BasicBlock;
import ir.values.Instruction;
import ir.values.constants.ConstInt;
import ir.values.instructions.BinaryOpInst;
import ir.values.instructions.PhiInst;
import passes.ir.IRPass;
import passes.ir.analysis.LoopAnalysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Stack;

public class ConstLoopUnrolling implements IRPass {

    private class LoopVariable {
        public Value variable;
        public int start;
        public int end;
        public int step = 0;
        public int loopTime;
    }

    HashMap<BasicBlock, LoopBB> bbmap;

    @Override
    public void runOnModule(Module module) {
        for (var func : module.functions) {
            bbmap = new HashMap<>();
            func.forEach(bb -> bbmap.put(bb, new LoopBB(bb)));
            LoopAnalysis.analysis(bbmap);

            var loops = LoopInfo.genLoopsWithBBs(bbmap.values());

            var topLoops = LoopInfo.genTopLoop(loops);

            topLoops.forEach(this::unrolling);
        }
    }

    private void unrolling(LoopAnalysis<LoopBB>.Loop loop) {
        if (!loop.getInnerLoops().isEmpty())
            loop.getInnerLoops().forEach(this::unrolling);

        loop.fillLoopInfo();

        if (isUnrollable(loop)) {
            var loopVar = getLoopVar(loop);

            System.out.println("Loop info: ");
            System.out.println("\tloop variable: " + loopVar.variable);
            System.out.println("\tinitial value: " + loopVar.start);
            System.out.println("\tfinal value: " + loopVar.end);
            System.out.println("\tstep: " + loopVar.step);
            System.out.println("\tloop time: " + loopVar.loopTime);

            if (loopVar.loopTime == 0) {
                removeLoop(loop);
            }
//            else if (loopVar.loopTime > 0){
//                unrollLoop(loop, loopVar);
//            }
        }
    }

    private boolean isUnrollable(LoopAnalysis<LoopBB>.Loop loop) {
        var exiting = loop.getExiting();
        var header = loop.getLoopHead();
        /* Only unroll the single condition loop */
        // TODO: 万一body第一句就是if...break的话识别不出来，不过这样也变相相当于多重条件
        if (exiting.contains(header) && header.getExitBlocks().stream().anyMatch(e -> loop.contains(e) && !exiting.contains(e))) {
            return true;
        }
        return false;
    }

    private LoopVariable getLoopVar(LoopAnalysis<LoopBB>.Loop loop) {
        var loopVar = new LoopVariable();

        var header = loop.getLoopHead();
        var cond = ((Instruction) header.getRawBasicBlock().getLastInst().getOperandAt(0));
        if (cond.isIcmp()) {
            // TODO: 目前为常数预测，可能处理变量？
            //<editor-fold desc="Find End">
            var icmp = ((BinaryOpInst) cond);
            var op1 = icmp.getOperandAt(0);
            var op2 = icmp.getOperandAt(1);
            Value cmpVar;
            ConstInt cmpConst;
            int varPos;
            if (op1 instanceof ConstInt) {
                cmpConst = ((ConstInt) op1);
                cmpVar = op2;
                varPos = 1;
            }
            else if (op2 instanceof ConstInt){
                cmpConst = ((ConstInt) op2);
                cmpVar = op1;
                varPos = 0;
            }
            else {
                System.out.println("ICMP非常量表达式");
                loopVar.loopTime = -1;
                return loopVar;
            }
            loopVar.end = cmpConst.getVal();
            //</editor-fold>

            // TODO: 处理条件判断为循环变量的数学表达式，目前只允许i<10这种形式
            if (!(cmpVar instanceof PhiInst)) {
                System.out.println("Cond非单一逻辑比较");
                loopVar.loopTime = -1;
                return loopVar;
            }
            loopVar.variable = ((PhiInst) cmpVar);

            var loopPhi = ((PhiInst) loopVar.variable);
            var loopDefs = new HashSet<Value>();
            var inits = new HashSet<Value>();
            for (var bb : loopPhi.getEntries()) {
                if (loop.contains(bbmap.get(bb)))
                    loopDefs.add(loopPhi.findValue(bb));
                else
                    inits.add(loopPhi.findValue(bb));
            }

            // TODO: 预测变量初始值？目前仅允许常量初始值
            Value tmp = inits.iterator().next();
            //<editor-fold desc="Find Start">
            if (inits.size() == 1 && tmp instanceof ConstInt) {
                loopVar.start = ((ConstInt) tmp).getVal();
            }
            else {
                System.out.println("初始值非常量");
                loopVar.loopTime = -1;
                return loopVar;
            }

            switch (icmp.getTag()) {
                case LT -> {
                    if ((varPos == 0 && loopVar.start >= loopVar.end) || (varPos == 1 && loopVar.end >= loopVar.start)) {
                        loopVar.loopTime = 0;
                        return loopVar;
                    }
                }
                case GT -> {
                    if ((varPos == 0 && loopVar.start <= loopVar.end) || (varPos == 1 && loopVar.end <= loopVar.start)) {
                        loopVar.loopTime = 0;
                        return loopVar;
                    }
                }
                case EQ -> {
                    if ((varPos == 0 && loopVar.start != loopVar.end) || (varPos == 1 && loopVar.end != loopVar.start)) {
                        loopVar.loopTime = 0;
                        return loopVar;
                    }
                }
                case NE -> {
                    if ((varPos == 0 && loopVar.start == loopVar.end) || (varPos == 1 && loopVar.end == loopVar.start)) {
                        loopVar.loopTime = 0;
                        return loopVar;
                    }
                }
                case LE -> {
                    if ((varPos == 0 && loopVar.start > loopVar.end) || (varPos == 1 && loopVar.end > loopVar.start)) {
                        loopVar.loopTime = 0;
                        return loopVar;
                    }
                }
                case GE -> {
                    if ((varPos == 0 && loopVar.start < loopVar.end) || (varPos == 1 && loopVar.end < loopVar.start)) {
                        loopVar.loopTime = 0;
                        return loopVar;
                    }
                }
            }
            //</editor-fold>

            // TODO: 预测多循环内入口phi，目前单入口，即循环中不存在continue
            //<editor-fold desc="Find step">
            Instruction dealing;
            tmp = loopDefs.iterator().next();
            if (loopDefs.size() == 1 && tmp instanceof Instruction) {
                dealing = ((Instruction) tmp);
            }
            else {
                System.out.println("循环内多phi入口");
                loopVar.loopTime = -1;
                return loopVar;
            }

            Stack<Instruction> defs = new Stack<>();
            while (!defs.contains(dealing)) {
                defs.add(dealing);
                // TODO: 多种运算？目前就支持ADD/SUB
                if (dealing.isAdd() || dealing.isSub()) {
                    op1 = dealing.getOperandAt(0);
                    op2 = dealing.getOperandAt(1);
                    if (op1 instanceof ConstInt && op2 instanceof Instruction && dealing.isAdd()) {
                        loopVar.step += ((ConstInt) op1).getVal();
                        dealing = (Instruction) op2;
                    }
                    else if (op2 instanceof ConstInt && op1 instanceof Instruction) {
                        int step = ((ConstInt) op2).getVal();
                        loopVar.step += (dealing.isAdd() ?step :-step);
                        dealing = (Instruction) op1;
                    }
                    else {
                        System.out.println("循环变量加减法非常量表达式");
                        loopVar.loopTime = -1;
                        return loopVar;
                    }
                }
                else if (dealing == loopPhi) {
                    break;
                }
                else {
                    System.out.println("循环变量表达式不是加减法");
                    loopVar.loopTime = -1;
                    return loopVar;
                }
            }
            //</editor-fold>

            if (loopVar.step != 0) {
                int loopTime = (loopVar.end - loopVar.start) / loopVar.step;
                loopVar.loopTime = Math.max(loopTime, 0);
                return loopVar;
            }
            else {
                System.out.println("步进为0");
            }
        }
        System.out.println("非ICMP");
        loopVar.loopTime = -1;
        return loopVar;
    }

    private void removeLoop(LoopAnalysis<LoopBB>.Loop loop) {
        if (!loop.getInnerLoops().isEmpty())
            loop.getInnerLoops().forEach(this::removeLoop);

        BasicBlock exit = loop.getExit().getRawBasicBlock();

        /* Replace all the use of the loop. Thanks to LCSSA, this is quite simple */
        for (var bb : loop.getBBs()) {
            var basicBlock = bb.getRawBasicBlock();
            for (var use : basicBlock.getUses()) {
                Instruction user = (Instruction) use.getUser();
                if (loop.contains(bbmap.get(user.getBB())))
                    continue;

                if (user.isPhi()) {
                    var phi = ((PhiInst) user);
                    if (phi.findValue(basicBlock) instanceof PhiInst) {
                        var usedPhi = ((PhiInst) phi.findValue(basicBlock));
                        var entry = usedPhi.getEntries().stream()
                                .filter(in -> !loop.contains(bbmap.get(in)))
                                .iterator().next();
                        var init = usedPhi.findValue(entry);
                        phi.addMapping(entry, init);

                        phi.removeMapping(basicBlock);
                    }
                    else {
                        throw new RuntimeException("ERROR when removeLoop: The usee is NOT a phi instruction!" + phi.findValue(basicBlock) + phi);
                    }
                }
                else if (user.isBr()){
                    user.setOperandAt(use.getOperandPos(), exit);
                }
            }
        }
    }

    private void unrollLoop(LoopAnalysis<LoopBB>.Loop loop, LoopVariable loopVar) {
        var header = loop.getLoopHead();
        var func = header.getRawBasicBlock().getFunc();

        for (int i=0; i<loopVar.loopTime-1; i++) {

        }
    }
}
