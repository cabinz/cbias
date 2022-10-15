package passes.ir.mem2reg;

import ir.Value;
import ir.values.Instruction;
import ir.values.instructions.MemoryInst;
import ir.values.instructions.PhiInst;
import passes.ir.analysis.IRelationAnalysis;

import java.util.*;

/**
 * BasicBlock with extra information.
 */
class BasicBlock extends passes.ir.BasicBlock implements Iterable<Instruction>, IRelationAnalysis<BasicBlock> {

    List<BasicBlock> previousBasicBlocks = new ArrayList<>();
    List<BasicBlock> followingBasicBlocks = new ArrayList<>();

    /**
     * Local variables which need to be defined in the previous block.
     */
    Set<MemoryInst.Alloca> npdVar = new HashSet<>();

    /**
     * Local variables which are changed in this block.
     */
    Set<MemoryInst.Alloca> definedVar = new HashSet<>();

    Map<MemoryInst.Alloca, PhiInst> importPhiMap = new HashMap<>();

    Map<MemoryInst.Alloca, Value> latestDefineMap = new HashMap<>();

    public BasicBlock(ir.values.BasicBlock basicBlock){
        super(basicBlock);
    }

    @Override
    public Iterator<Instruction> iterator() {
        return getRawBasicBlock().iterator();
    }

    public void addEntryBlock(BasicBlock entryBlock){
        previousBasicBlocks.add(entryBlock);
    }

    public void setExitBlocks(List<BasicBlock> exitBlocks){
        this.followingBasicBlocks = exitBlocks;
    }

    /**
     * Filter unpromotable alloca instructions.
     * <p>
     * Alloca instructions which are used as address will be filtered.
     *
     * @param allocaInstSet The set of instruction to be filtered.
     */
    public void filterUnpromotableAllocaInst(Set<MemoryInst.Alloca> allocaInstSet){
        rawBasicBlock.forEach(instruction -> {
            if(!instruction.isLoad() && !instruction.isStore()){
                instruction.getOperands().forEach(use -> {
                    if(use.getUsee() instanceof MemoryInst.Alloca){
                        allocaInstSet.remove((MemoryInst.Alloca) use.getUsee());
                    }
                });
            }
        });
    }

}
