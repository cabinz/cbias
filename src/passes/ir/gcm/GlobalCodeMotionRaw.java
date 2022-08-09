package passes.ir.gcm;

import ir.Module;
import ir.Use;
import ir.values.Function;
import ir.values.instructions.*;
import passes.ir.gcm.domtree.DomTree;

import java.util.*;
import java.util.function.Consumer;

public class GlobalCodeMotionRaw {

    private GlobalCodeMotionRaw() {
    }

    private final Map<ir.values.Instruction, Instruction> instructionMap = new HashMap<>();
    private final Map<ir.values.BasicBlock, BasicBlock> basicBlockMap = new HashMap<>();

    private BasicBlock functionEntry;

    private void doPrepareWork(Function function) {
        // Build basic information
        function.forEach(basicBlock -> {
            basicBlockMap.put(basicBlock, new BasicBlock(basicBlock));
        });

        // Remove instructions from BasicBlocks.
        var instructionSet = new InstructionSet();
        function.forEach(basicBlock -> basicBlock.forEach(instruction -> {
            if (instruction instanceof UnaryOpInst || instruction instanceof BinaryOpInst || instruction instanceof CastInst || instruction instanceof GetElemPtrInst) {
                if (instructionSet.add(instruction)) {
                    instruction.removeSelf();
                    instructionMap.put(instruction, new Instruction(instruction));
                } else {
                    var prevInst = instructionSet.get(instruction);
                    instruction.replaceSelfTo(prevInst);
                }
            }
        }));

        // Function entry
        functionEntry = basicBlockMap.get(function.getEntryBB());

    }

    private void analysisFunction() {
        // Run dom tree analysis and loop analysis
        DomTree.analysis(basicBlockMap);
        LoopAnalysis.analysis(this);
    }

    private void generateEarlyPlacement(Instruction instruction) {
        if (instruction.getEarlyPlacement() != null) return;
        BasicBlock earlyPlacement = functionEntry;
        for (Use use : instruction.getRawInstruction().getOperands()) {
            var usee = use.getUsee();
            if (!(usee instanceof ir.values.Instruction)) continue;
            var useeInst = (ir.values.Instruction) usee;
            BasicBlock newLimit;
            if (useeInst.getBB() == null) {
                var wrappedUsee = instructionMap.get(useeInst);
                if (wrappedUsee.getLatePlacement() == null) {
                    generateEarlyPlacement(wrappedUsee);
                }
                newLimit = wrappedUsee.getEarlyPlacement();
            } else {
                newLimit = basicBlockMap.get(useeInst.getBB());
            }
            if (earlyPlacement == null) {
                earlyPlacement = newLimit;
            } else {
                if (newLimit.getDomDepth() > earlyPlacement.getDomDepth()) {
                    earlyPlacement = newLimit;
                }
            }
        }
        instruction.setEarlyPlacement(earlyPlacement);
    }

    private Collection<Instruction> getEarlyTopoOrder(Collection<Instruction> instructions_){
        Set<Instruction> instructions = new HashSet<>(instructions_);
        Map<Instruction, Integer> pendingInstructions = new HashMap<>();
        Queue<Instruction> availableInstructions = new ArrayDeque<>();
        for (Instruction instruction : instructions) {
            int pendingCount = 0;
            for (Use use : instruction.getRawInstruction().getOperands()) {
                var usee = use.getUsee();
                if(!(usee instanceof ir.values.Instruction)) continue;
                if(!instructions.contains(instructionMap.get(usee))) continue;
                pendingCount += 1;
            }
            if(pendingCount==0){
                availableInstructions.add(instruction);
            }else{
                pendingInstructions.put(instruction, pendingCount);
            }
        }
        var result = new ArrayList<Instruction>();
        while (!availableInstructions.isEmpty()){
            var instruction = availableInstructions.remove();
            result.add(instruction);
            for (Use use : instruction.getRawInstruction().getUses()) {
                var user = instructionMap.get((ir.values.Instruction) use.getUser());
                if(instructions.contains(user)){
                    var newCount = pendingInstructions.get(user)-1;
                    if(newCount==0){
                        availableInstructions.add(user);
                        pendingInstructions.remove(user);
                    }else{
                        pendingInstructions.put(user, newCount);
                    }
                }
            }
        }
        if(!pendingInstructions.isEmpty()){
            throw new RuntimeException("Unable to generate topo order. (Maybe loops in the use graph)");
        }
        return result;
    }

    private void scheduleEarly() {
        getEarlyTopoOrder(instructionMap.values()).forEach(this::generateEarlyPlacement);
    }

    private void dfsForFastJumpInfo(BasicBlock basicBlock) {
        for (int i = 0; i < basicBlock.getFastJumps().size(); i++) {
            var l1f = basicBlock.getFastJumps().get(i);
            if (l1f.getFastJumps().size() > i) {
                basicBlock.getFastJumps().add(l1f.getFastJumps().get(i));
                var l1l = basicBlock.getShallowestBBs().get(i);
                var l2l = l1f.getShallowestBBs().get(i);
                basicBlock.getShallowestBBs().add(l1l.getLoopDepth() <= l2l.getLoopDepth() ? l1l : l2l);
            }
        }
        for (BasicBlock domSon : basicBlock.getDomSons()) {
            domSon.getFastJumps().add(basicBlock);
            domSon.getShallowestBBs().add(basicBlock);
            dfsForFastJumpInfo(domSon);
        }
    }

    private BasicBlock getLCAForBlocks(BasicBlock u, BasicBlock v) {
        if (u.getDomDepth() < v.getDomDepth()) {
            BasicBlock tmp = u;
            u = v;
            v = tmp;
        }
        int deltaDepth = u.getDomDepth() - v.getDomDepth();
        for (int i = 0; deltaDepth > 0; i++) {
            if ((deltaDepth & (1 << i)) != 0) {
                u = u.getFastJumps().get(i);
                deltaDepth -= (1 << i);
            }
        }
        if (u == v) return u;
        for (int i = u.getFastJumps().size() - 1; i >= 0; i--) {
            if (u.getFastJumps().get(i) != v.getFastJumps().get(i)) {
                u = u.getFastJumps().get(i);
                v = v.getFastJumps().get(i);
            }
        }
        return u.getFastJumps().get(0);
    }

    private void generateLatePlacement(Instruction instruction) {
        BasicBlock latePlacement = null;
        for (Use use : instruction.getRawInstruction().getUses()) {
            var user = (ir.values.Instruction) use.getUser();
            List<BasicBlock> newLimits = new ArrayList<>();
            if (user.getBB() == null) {
                var userInst = instructionMap.get(user);
                newLimits.add(userInst.getLatePlacement());
            } else {
                if(user instanceof PhiInst){
                    var phiInst = (PhiInst) user;
                    for (ir.values.BasicBlock entry : phiInst.getEntries()) {
                        var value = phiInst.findValue(entry);
                        if(value==instruction.getRawInstruction()){
                            newLimits.add(basicBlockMap.get(entry));
                        }
                    }
                }else{
                    newLimits.add(basicBlockMap.get(user.getBB()));
                }
            }
            for (BasicBlock newLimit : newLimits) {
                if(newLimit==null) continue;
                if (latePlacement == null) {
                    latePlacement = newLimit;
                } else {
                    latePlacement = getLCAForBlocks(latePlacement, newLimit);
                }
            }
        }
        instruction.setLatePlacement(latePlacement);
    }

    private Collection<Instruction> getLateTopoOrder(Collection<Instruction> instructions_){
        Set<Instruction> instructions = new HashSet<>(instructions_);
        Map<Instruction, Integer> pendingInstructions = new HashMap<>();
        Queue<Instruction> availableInstructions = new ArrayDeque<>();
        for (Instruction instruction : instructions) {
            int pendingCount = 0;
            for (Use use : instruction.getRawInstruction().getUses()) {
                var user = use.getUser();
                if(!(user instanceof ir.values.Instruction)) continue;
                if(!instructions.contains(instructionMap.get(user))) continue;
                pendingCount += 1;
            }
            if(pendingCount==0){
                availableInstructions.add(instruction);
            }else{
                pendingInstructions.put(instruction, pendingCount);
            }
        }
        var result = new ArrayList<Instruction>();
        while (!availableInstructions.isEmpty()){
            var instruction = availableInstructions.remove();
            result.add(instruction);
            for (Use use : instruction.getRawInstruction().getOperands()) {
                var usee = use.getUsee();
                if(!(usee instanceof ir.values.Instruction)) continue;
                var useeInst = instructionMap.get(usee);
                if(instructions.contains(useeInst)){
                    var newCount = pendingInstructions.get(useeInst)-1;
                    if(newCount==0){
                        availableInstructions.add(useeInst);
                        pendingInstructions.remove(useeInst);
                    }else{
                        pendingInstructions.put(useeInst, newCount);
                    }
                }
            }
        }
        if(!pendingInstructions.isEmpty()){
            throw new RuntimeException("Unable to generate topo order. (Maybe loops in the use graph)");
        }
        return result;
    }

    private void scheduleLate() {
        dfsForFastJumpInfo(functionEntry);
        getLateTopoOrder(instructionMap.values()).forEach(this::generateLatePlacement);
        instructionMap.values().removeIf(instruction -> instruction.getLatePlacement()==null);
    }

    private void dfsForPlacement(BasicBlock basicBlock) {
        // Place instructions
        Queue<ir.values.Instruction> availableInstructions = new ArrayDeque<>();
        Map<ir.values.Instruction, Integer> pendingCountMap = new HashMap<>();
        for (Instruction instruction : basicBlock.getPendingInstructions()) {
            int pendingCount = 0;
            for (Use use : instruction.getRawInstruction().getOperands()) {
                var usee = use.getUsee();
                if (usee instanceof ir.values.Instruction) {
                    var inst = (ir.values.Instruction) usee;
                    if (inst.getBB() == null || inst.getBB() == basicBlock.getRawBasicBlock()) {
                        pendingCount++;
                    }
                }
            }
            if (pendingCount == 0) {
                availableInstructions.add(instruction.getRawInstruction());
            } else {
                pendingCountMap.put(instruction.getRawInstruction(), pendingCount);
            }
        }
        ir.values.Instruction lastInst = null;
        for (ir.values.Instruction instruction : basicBlock.getRawBasicBlock().getInstructions()) {
            if (instruction instanceof PhiInst || instruction instanceof MemoryInst.Alloca) {
                lastInst = instruction;
            }
        }
        if (lastInst == null) {
            for (ir.values.Instruction availableInstruction : availableInstructions) {
                basicBlock.getRawBasicBlock().insertAtFront(availableInstruction);
            }
            availableInstructions.clear();
        }
        Deque<ir.values.Instruction> queue = new ArrayDeque<>(basicBlock.getRawBasicBlock().getInstructions());
        while (!queue.isEmpty()) {
            var instruction = queue.remove();
            for (Use use : instruction.getUses()) {
                var user = (ir.values.Instruction) use.getUser();
                if (pendingCountMap.containsKey(user)) {
                    var newCount = pendingCountMap.get(user) - 1;
                    if (newCount == 0) {
                        availableInstructions.add(user);
                        pendingCountMap.remove(user);
                    } else {
                        pendingCountMap.put(user, newCount);
                    }
                }
            }
            if (instruction == lastInst) {
                lastInst = null;
            }
            if (lastInst == null) {
                for (ir.values.Instruction availableInstruction : availableInstructions) {
                    availableInstruction.insertAfter(instruction);
                    queue.addFirst(availableInstruction);
                }
                availableInstructions.clear();
            }
        }

        if(!pendingCountMap.isEmpty()){
            System.out.println(pendingCountMap);
            throw new RuntimeException("Unable to place the rest instructions.");
        }

        // Place sons
        for (BasicBlock domSon : basicBlock.getDomSons()) {
            dfsForPlacement(domSon);
        }

    }

    private void placeBackInstructions() {
        scheduleEarly();
        scheduleLate();
        instructionMap.values().forEach(instruction -> {
            var earlyBB = instruction.getEarlyPlacement();
            var lateBB = instruction.getLatePlacement();
            var targetBB = lateBB;
            for (int i = 0; (1 << i) <= lateBB.getDomDepth() - earlyBB.getDomDepth(); i++) {
                if ((lateBB.getDomDepth() - earlyBB.getDomDepth() & (1 << i)) != 0) {
                    var newTargetBB = lateBB.getShallowestBBs().get(i);
                    if (newTargetBB.getLoopDepth() < targetBB.getLoopDepth()) {
                        targetBB = newTargetBB;
                    }
                    lateBB = lateBB.getFastJumps().get(i);
                }
            }
            targetBB.addPendingInstructions(instruction);
        });
        dfsForPlacement(functionEntry);
    }

    private void __optimize__(Function function) {
        doPrepareWork(function);
        analysisFunction();
        placeBackInstructions();
    }

    public Map<ir.values.Instruction, Instruction> getInstructionMap() {
        return instructionMap;
    }

    public Map<ir.values.BasicBlock, BasicBlock> getBasicBlockMap() {
        return basicBlockMap;
    }

    public static void optimize(Module module) {
        module.functions.forEach(function -> (new GlobalCodeMotionRaw()).__optimize__(function));
    }

}
