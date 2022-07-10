package passes.ir.mem2reg;

import ir.values.instructions.MemoryInst;
import ir.values.instructions.TerminatorInst;

import java.util.*;

/**
 * A function which is contains more information about its context.
 */
class Function extends passes.ir.Function implements Iterable<BasicBlock> {

    Map<ir.values.BasicBlock, passes.ir.mem2reg.BasicBlock> basicBlockMap = new HashMap<>();

    public Function(ir.values.Function rawFunction){
        super(rawFunction);
        rawFunction.forEach(basicBlock -> basicBlockMap.put(basicBlock,new BasicBlock(basicBlock)));
        basicBlockMap.values().forEach(basicBlock -> {
            var lastInstruction = basicBlock.getRawBasicBlock().getLastInst();
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
        for(var instruction: rawFunction.getEntryBB()){
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
