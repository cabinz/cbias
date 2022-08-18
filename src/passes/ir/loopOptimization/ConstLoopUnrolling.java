package passes.ir.loopOptimization;

import frontend.IREmitter;
import ir.Module;
import ir.Use;
import ir.Value;
import ir.types.IntegerType;
import ir.types.PointerType;
import ir.values.BasicBlock;
import ir.values.Constant;
import ir.values.Function;
import ir.values.Instruction;
import ir.values.constants.ConstInt;
import ir.values.instructions.*;
import passes.PassManager;
import passes.ir.DummyValue;
import passes.ir.IRPass;
import passes.ir.analysis.LoopAnalysis;
import passes.ir.constant_derivation.ConstantDerivation;

import java.io.IOException;
import java.util.*;

public class ConstLoopUnrolling implements IRPass {

    private class LoopVariable {
        public Value variable;
        public int start;
        public int end;
        public int step = 0;
        public int loopTime;
    }

    HashMap<BasicBlock, LoopBB> loopbbMap;

    private boolean changed = true;
//    public static boolean printInfo = true;
    public static boolean printInfo = false;

    @Override
    public void runOnModule(Module module) {
        if (printInfo) {
            try {
                (new IREmitter("test/beforeUnroll.ll")).emit(module);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (var func : module.functions) {
            int counter = 0;
            while (changed) {
                changed = false;
                loopbbMap = new HashMap<>();
                func.forEach(bb -> loopbbMap.put(bb, new LoopBB(bb)));
                LoopAnalysis.analysis(loopbbMap);

                var loops = LoopInfo.genLoopsWithBBs(loopbbMap.values());

                var topLoops = LoopInfo.genTopLoop(loops);

                topLoops.forEach(this::unrolling);

                if (printInfo) {
                    try {
                        System.out.println("unroll done");
                        (new IREmitter("test/unroll"+ counter +".ll")).emit(module);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                PassManager.getInstance().basicOptimize(module);

                if (printInfo) {
                    try {
                        System.out.println("D done");
                        (new IREmitter("test/derivation"+ counter +".ll")).emit(module);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    counter++;
                }
            }
        }
    }

    private void unrolling(LoopAnalysis<LoopBB>.Loop loop) {
        if (!loop.getInnerLoops().isEmpty())
            loop.getInnerLoops().forEach(this::unrolling);

        loop.fillLoopInfo();

        if (isUnrollable(loop)) {
            var loopVar = getLoopVar(loop);

            if (printInfo) {
                System.out.println("Loop info: ");
                System.out.println("\tloop variable: " + loopVar.variable);
                System.out.println("\tinitial value: " + loopVar.start);
                System.out.println("\tfinal value: " + loopVar.end);
                System.out.println("\tstep: " + loopVar.step);
                System.out.println("\tloop time: " + loopVar.loopTime);
            }

            if (loopVar.loopTime == 0) {
                if (printInfo) System.out.println("无用循环删除");
                removeLoop(loop);
            }
            else if (loopVar.loopTime > 0){
                if (printInfo) System.out.println("循环展开");
                unrollLoop(loop, loopVar);
            }
        }
    }

    private boolean isUnrollable(LoopAnalysis<LoopBB>.Loop loop) {
        var exiting = loop.getExiting();
        var header = loop.getLoopHead();
        /* Only unroll the single condition loop */
        // TODO: 万一body第一句就是if...break的话识别不出来，不过这样也变相相当于多重条件
        return exiting.contains(header) && header.getExitBlocks().stream().anyMatch(e -> loop.contains(e) && !exiting.contains(e));
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
                if (printInfo) System.out.println("ICMP非常量表达式");
                loopVar.loopTime = -1;
                return loopVar;
            }
            loopVar.end = cmpConst.getVal();
            //</editor-fold>

            // TODO: 处理条件判断为循环变量的数学表达式，目前只允许i<10这种形式
            if (!(cmpVar instanceof PhiInst)) {
                if (printInfo) System.out.println("Cond非单一逻辑比较");
                loopVar.loopTime = -1;
                return loopVar;
            }
            loopVar.variable = ((PhiInst) cmpVar);

            var loopPhi = ((PhiInst) loopVar.variable);
            var loopDefs = new HashSet<Value>();
            var inits = new HashSet<Value>();
            for (var bb : loopPhi.getEntries()) {
                if (loop.contains(loopbbMap.get(bb)))
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
                if (printInfo) System.out.println("初始值非常量");
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
                if (printInfo) System.out.println("循环内多phi入口");
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
                        if (printInfo) System.out.println("循环变量加减法非常量表达式");
                        loopVar.loopTime = -1;
                        return loopVar;
                    }
                }
                else if (dealing == loopPhi) {
                    break;
                }
                else {
                    if (printInfo) System.out.println("循环变量表达式不是加减法");
                    loopVar.loopTime = -1;
                    return loopVar;
                }
            }
            //</editor-fold>

            if (loopVar.step != 0) {
                int range = loopVar.end - loopVar.start;
                int loopTime = range / loopVar.step + (range%loopVar.step==0 ?0 :1);
                loopVar.loopTime = Math.max(loopTime, 0);
                return loopVar;
            }
            else {
                if (printInfo) System.out.println("步进为0");
            }
        }
        if (printInfo) System.out.println("非ICMP");
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
                if (loop.contains(loopbbMap.get(user.getBB())))
                    continue;

                if (user.isPhi()) {
                    var phi = ((PhiInst) user);
                    if (phi.findValue(basicBlock) instanceof PhiInst) {
                        var usedPhi = ((PhiInst) phi.findValue(basicBlock));
                        var entry = usedPhi.getEntries().stream()
                                .filter(in -> !loop.contains(loopbbMap.get(in)))
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

    class Loop4Copy {
        public BasicBlock header;
        public LinkedList<BasicBlock> bbs = new LinkedList<>();
        public LinkedList<BasicBlock> exiting = new LinkedList<>();
        public LinkedList<BasicBlock> latch = new LinkedList<>();
    }

    private void unrollLoop(LoopAnalysis<LoopBB>.Loop loop, LoopVariable loopVar) {
        if (changed)
            return;
        else
            changed = true;

        /* 因为循环是单入口单出口，所以展开比较方便 */
        var header = loop.getLoopHead();
        var func = header.getRawBasicBlock().getFunc();

        var exit = loop.getExit().getRawBasicBlock();

        HashMap<Value, Value> loopVarMap = new HashMap<>();
        for (var inst : header.getRawBasicBlock()) {
            if (inst.isPhi()) {
                Value init = null;
                var phi = ((PhiInst) inst);
                for (var bb : phi.getEntries()) {
                    if (!loop.contains(loopbbMap.get(bb))) {
                        init = phi.findValue(bb);
                        break;
                    }
                }
                loopVarMap.put(inst, init);
            }
            else
                break;
        }

        HashSet<BasicBlock> entris = new HashSet<>();
        entris.add(loop.getEntry().getRawBasicBlock());

        for (int i=loopVar.loopTime-1; i>=0; i--) {
            var cloned
                    = cloneLoop(loop, loopVarMap, i);
            cloned.bbs.forEach(func::addBB);

            for (var entry : entris) {
                branchTo(entry, cloned.header);
            }
            entris.clear();
            entris.addAll(cloned.latch);

            for (var exiting : cloned.exiting) {
                branchTo(exiting, exit);
            }
        }

        for (var entry : entris) {
            branchTo(entry, exit);
        }

        for (var bb : loop.getBBs()) {
            for (var use : bb.getRawBasicBlock().getUses()) {
                if (use.getUser() instanceof PhiInst) {
                    var phi = ((PhiInst) use.getUser());
                    phi.removeMapping(bb.getRawBasicBlock());
                }
            }
        }
    }

    private Loop4Copy cloneLoop(LoopAnalysis<LoopBB>.Loop loop, HashMap<Value, Value> loopVarMap, int i) {
        var cloned = new Loop4Copy();
        int counter = 0;

        var allBB = loop.getAllBBs();
        var instMap = new HashMap<Value, Value>();
        var bbMap = new HashMap<LoopBB, BasicBlock>();

        var exiting = loop.getExiting();
        var latch = loop.getLatch();
        var header = loop.getLoopHead();

        //<editor-fold desc="Clone blocks">
        for (var bb : allBB) {
            for (var inst : bb.getRawBasicBlock()) {
                instMap.put(inst, new DummyValue(inst.getType()));
            }
            var clonedBB = new BasicBlock("clonedLoopBlock_" + i + "_" + counter++);
            bbMap.put(bb, clonedBB);
            cloned.bbs.add(clonedBB);
            if (bb == header)
                cloned.header = clonedBB;
            else if (exiting.contains(bb))
                cloned.exiting.add(clonedBB);
            if (latch.contains(bb))
                cloned.latch.add(clonedBB);
        }

        for (var bb : allBB) {
            for (var inst : bb.getRawBasicBlock()) {
                var curBB = bbMap.get(bb);
                var dummy = instMap.get(inst);
                if (loopVarMap.containsKey(inst)){
                    var toReplace = loopVarMap.get(inst);
                    dummy.replaceSelfTo(toReplace);
                    instMap.put(inst, toReplace);
                }
                else {
                    var clonedInst = cloneInst(inst, instMap, bbMap);
                    dummy.replaceSelfTo(clonedInst);
                    instMap.put(inst, clonedInst);
                    curBB.insertAtEnd(clonedInst);
                }
            }
        }
        //</editor-fold>

        //<editor-fold desc="Adjust header">
        BasicBlock body = bbMap.get(loop.getBodyEntry());
        var loopBr = bbMap.get(header).getLastInst();
        Instruction loopCmp = (Instruction) loopBr.getOperandAt(0);
        loopBr.markWasted();
        loopCmp.markWasted();

        bbMap.get(header).insertAtEnd(new TerminatorInst.Br(body));
        //</editor-fold>

        //<editor-fold desc="Adjust loop variable to the new value">
        loopVarMap.forEach((k, origin) -> {
            var phi = ((PhiInst) k);
            loopVarMap.put(k, instMap.get(phi.findValue(latch.iterator().next().getRawBasicBlock())));
        });
        //</editor-fold>

        //<editor-fold desc="Adjust the use outside the loop">
        for (var bb : exiting) {
            for (var use : bb.getRawBasicBlock().getUses()) {
                if (bb != header && use.getUser() instanceof PhiInst) {
                    var phi = ((PhiInst) use.getUser());
                    var originValue = phi.findValue(bb.getRawBasicBlock());
                    if (originValue instanceof Constant)
                        phi.addMapping(bbMap.get(bb), originValue);
                    else
                        phi.addMapping(bbMap.get(bb), instMap.get(originValue));
                }
            }
        }

        if (i==0) {
            for (var inst : loop.getExit().getRawBasicBlock()) {
                if (inst instanceof PhiInst) {
                    var phi = ((PhiInst) inst);
                    for (var bb : cloned.latch) {
                        var val = phi.findValue(header.getRawBasicBlock());
                        if (loopVarMap.containsKey(val))
                            phi.addMapping(bb, loopVarMap.get(val));
                        else
                            phi.addMapping(bb, instMap.get(val));
                    }
                }
                else
                    break;
            }
        }
        //</editor-fold>

        return cloned;
    }

    /**
     * Cloned from {@link passes.ir.inline.ClonedFunction#cloneOps(Instruction)}
     */
    private Map<Integer,Value> cloneOps(Instruction source, Map<Value, Value> valueMap, Map<LoopBB, BasicBlock> bbMap){
        Map<Integer,Value> map = new HashMap<>();
        for (Use use : source.getOperands()) {
            var usee = use.getUsee();
            Value value;
            if(usee instanceof BasicBlock){
                value = bbMap.getOrDefault(loopbbMap.get(usee), ((BasicBlock) usee));
            }else{
                value = valueMap.getOrDefault(usee, usee);
            }
            map.put(use.getOperandPos(), value);
        }
        return map;
    }

    /**
     * Cloned from {@link passes.ir.inline.ClonedFunction#cloneInst(Instruction)}
     */
    private Instruction cloneInst(Instruction source, Map<Value, Value> valueMap, Map<LoopBB, BasicBlock> bbMap){
        var type = source.getType();
        var category = source.getTag();
        var ops = cloneOps(source, valueMap, bbMap);
        if(source instanceof UnaryOpInst){
            return new UnaryOpInst(type,category,ops.get(0));
        }
        if(source instanceof BinaryOpInst){
            return new BinaryOpInst(type,category,ops.get(0),ops.get(1));
        }
        if(source instanceof CallInst){
            int argNum = ops.size()-1;
            ArrayList<Value> args = new ArrayList<>();
            for(int i=1;i<=argNum;i++){
                args.add(ops.get(i));
            }
            return new CallInst((Function) ops.get(0),args);
        }
        if(source instanceof CastInst){
            if(source instanceof CastInst.ZExt){
                return new CastInst.ZExt(ops.get(0));
            }
            if(source instanceof CastInst.Fptosi){
                return new CastInst.Fptosi(ops.get(0),(IntegerType) type);
            }
            if(source instanceof CastInst.Sitofp){
                return new CastInst.Sitofp(ops.get(0));
            }
            if(source instanceof CastInst.Bitcast){
                return new CastInst.Bitcast(ops.get(0),type);
            }
        }
        if(source instanceof GetElemPtrInst){
            var indexNum = ops.size()-1;
            ArrayList<Value> indices = new ArrayList<>();
            for(int i=1;i<=indexNum;i++){
                indices.add(ops.get(i));
            }
            return new GetElemPtrInst(type, ops.get(0),indices);
        }
        if(source instanceof MemoryInst){
            if(source instanceof MemoryInst.Store){
                return new MemoryInst.Store(ops.get(0),ops.get(1));
            }
            if(source instanceof MemoryInst.Load){
                return new MemoryInst.Load(type,ops.get(0));
            }
            if(source instanceof MemoryInst.Alloca){
                return new MemoryInst.Alloca(((PointerType)type).getPointeeType());
            }
        }
        if(source instanceof PhiInst){
            var srcPhi = (PhiInst) source;
            var phi = new PhiInst(type);
            for (BasicBlock entry : srcPhi.getEntries()) {
                var value = srcPhi.findValue(entry);
                phi.addMapping(bbMap.getOrDefault(loopbbMap.get(entry), entry), valueMap.getOrDefault(value,value));
            }
            return phi;
        }
        if(source instanceof TerminatorInst){
            if(source instanceof TerminatorInst.Br){
                if(((TerminatorInst.Br)source).isCondJmp()){
                    return new TerminatorInst.Br(ops.get(0), (BasicBlock) ops.get(1), (BasicBlock) ops.get(2));
                }else{
                    return new TerminatorInst.Br((BasicBlock) ops.get(0));
                }
            }
            if(source instanceof TerminatorInst.Ret){
                if (source.getNumOperands() == 0)
                    return new TerminatorInst.Ret();
                else
                    return new TerminatorInst.Ret(ops.get(0));
            }
        }
        throw new RuntimeException("Unable to clone instruction of type "+source.getClass());
    }

    private void branchTo(BasicBlock src, BasicBlock dst) {
        /* 因为其他优化不会把退出处理成条件跳转才可以这样做 */
        var last = src.getLastInst();
        if (last.isBr()) {
            last.setOperandAt(0, dst);
        }
    }
}
