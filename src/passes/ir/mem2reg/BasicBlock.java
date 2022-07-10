package passes.ir.mem2reg;

import ir.Value;
import ir.values.Instruction;
import ir.values.instructions.MemoryInst;
import ir.values.instructions.PhiInst;

import java.util.*;
import java.util.function.Consumer;

class BasicBlock extends passes.ir.BasicBlock implements Iterable<Instruction> {

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

    public void addPreviousBasicBlock(BasicBlock basicBlock){
        previousBasicBlocks.add(basicBlock);
    }

    public void setFollowingBasicBlocks(List<BasicBlock> followingBasicBlocks){
        this.followingBasicBlocks = followingBasicBlocks;
    }

    /**
     * Filter unpromotable alloca instructions.
     *
     * Alloca instructions which are used as address will be filtered.
     *
     * @param allocaInstSet The set of instruction to be filtered.
     */
    public void filterUnpromotableAllocaInst(Set<MemoryInst.Alloca> allocaInstSet){
        rawBasicBlock.forEach(instruction -> {
            if(!instruction.isLoad() && !instruction.isStore()){
                instruction.operands.forEach(use -> {
                    if(use.getValue() instanceof MemoryInst.Alloca){
                        allocaInstSet.remove((MemoryInst.Alloca) use.getValue());
                    }
                });
            }
        });
    }

}