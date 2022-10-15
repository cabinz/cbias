package passes.ir.mem2reg;

import ir.values.instructions.MemoryInst;
import passes.ir.analysis.RelationAnalysis;

import java.util.*;

/**
 * Function with extra information.
 */
class Function extends passes.ir.Function implements Iterable<BasicBlock> {

    Map<ir.values.BasicBlock, passes.ir.mem2reg.BasicBlock> basicBlockMap = new HashMap<>();

    public Function(ir.values.Function rawFunction){
        super(rawFunction);
        rawFunction.forEach(basicBlock -> basicBlockMap.put(basicBlock,new BasicBlock(basicBlock)));
        RelationAnalysis.analysisBasicBlocks(basicBlockMap);
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
     * <p>
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
