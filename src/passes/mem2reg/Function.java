package passes.mem2reg;

import ir.values.instructions.MemoryInst;
import ir.values.instructions.TerminatorInst;

import java.util.*;
import java.util.function.Consumer;

/**
 * A function which is contains more information about its context.
 */
class Function implements Iterable<BasicBlock> {

    ir.values.Function function;

    Map<ir.values.BasicBlock, passes.mem2reg.BasicBlock> basicBlockMap = new HashMap<>();

    public Function(ir.values.Function function){
        this.function = function;
        function.forEach(basicBlock -> basicBlockMap.put(basicBlock,new BasicBlock(basicBlock)));
        basicBlockMap.values().forEach(basicBlock -> {
            var lastInstruction = basicBlock.getUnwrappedBB().getLastInst();
            if(lastInstruction.isBr()){
                var br = (TerminatorInst.Br) lastInstruction;
                List<BasicBlock> followingBasicBlocks = new ArrayList<>();
                if(br.isCondJmp()){
                    followingBasicBlocks.add(basicBlockMap.get((ir.values.BasicBlock) br.getOperandAt(1)));
                    followingBasicBlocks.add(basicBlockMap.get((ir.values.BasicBlock) br.getOperandAt(2)));
                }else{
                    followingBasicBlocks.add(basicBlockMap.get((ir.values.BasicBlock) br.getOperandAt(0)));
                }
                followingBasicBlocks.forEach(followingBasicBlock -> followingBasicBlock.addPreviousBasicBlock(basicBlock));
                basicBlock.setFollowingBasicBlocks(followingBasicBlocks);
            }
        });
    }

    @Override
    public Iterator<BasicBlock> iterator() {
        return getBasicBlocks().iterator();
    }

    @Override
    public void forEach(Consumer<? super BasicBlock> action) {
        getBasicBlocks().forEach(action);
    }

    @Override
    public Spliterator<BasicBlock> spliterator() {
        return getBasicBlocks().spliterator();
    }

    public Collection<BasicBlock> getBasicBlocks(){
        return basicBlockMap.values();
    }

    /**
     * Returns the promotable alloca instructions in this function.
     *
     * An alloca is unpromotable iff it is used as an address.(aka. used outside Load and Store)
     *
     * @return The set of promotable instructions.
     */
    public Collection<MemoryInst.Alloca> getPromotableAllocaInstructions(){
        Set<MemoryInst.Alloca> set = new HashSet<>();
        for(var instruction: function.getEntryBB()){
            if(instruction.isAlloca()){
                set.add((MemoryInst.Alloca) instruction);
            }else{
                break;
            }
        }
        forEach(basicBlock -> basicBlock.filterUnpromotableAllocaInst(set));
        return set;
    }

}
