package ir.values;

import ir.Value;
import ir.Type;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A basic block is simply a container of instructions that execute sequentially.
 * Basic blocks are Values because they are referenced by instructions such as branches.
 * A well-formed basic block is formed of a list of non-terminating instructions followed
 * by a single terminator instruction.
 * <br>
 * Type for a BasicBlock is Type.LabelType because the basic block represents a label
 * to which a branch can jump.
 * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/BasicBlock.h#L58">
 *     LLVM IR BasicBlock Source</a>
 */
public class BasicBlock extends Value implements Iterable<Instruction>{

    //<editor-fold desc="Fields">
    private final ArrayList<BasicBlock> predecessors = new ArrayList<>();
    private final ArrayList<BasicBlock> successors = new ArrayList<>();
    public final LinkedList<Instruction> instructions = new LinkedList<>();
    //</editor-fold>

    //<editor-fold desc="Constructors">
    public BasicBlock(String name) {
        super(Type.LabelType.getType());

        this.setName(name);
    }

    /**
     * Retrieve the last instruction in the basic block.
     * @return The last instruction. Null if it's an empty block.
     */
    public Instruction getLastInst() {
        return this.instructions.isEmpty() ? null : this.instructions.getLast();
    }

    /**
     * Insert an instruction at the end of the basic block.
     * @param inst The instruction to be inserted.
     */
    public void insertAtEnd(Instruction inst) {
        this.instructions.addLast(inst);
    }

    /**
     * Insert an instruction at the beginning of the basic block.
     * @param inst The instruction to be inserted.
     */
    public void insertAtFront(Instruction inst) {
        this.instructions.addFirst(inst);
    }

    public Iterator<Instruction> iterator() {
        return instructions.iterator();
    }

    public void addPredecessor(BasicBlock pre) {
        this.predecessors.add(pre);
    }

    public void addSuccessor(BasicBlock suc) {
        this.successors.add(suc);
    }
    //</editor-fold>
}
