package ir.values;

import ir.Value;
import ir.types.LabelType;

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

    public final LinkedList<Instruction> instructions = new LinkedList<>();

    public BasicBlock(String name) {
        super(LabelType.getType());

        this.setName(name);
    }

    /**
     * Returns true if the BasicBlock contains no Instruction.
     * @return true if the BasicBlock contains no Instruction.
     */
    public boolean isEmpty() {
        return instructions.isEmpty();
    }

    /**
     * Removes a specified instruction from the Basicblock.
     * All the related Use links will be simultaneously wiped out.
     * @param inst The Instruction to be removed.
     * @return true if successfully remove an inst contained in the BB.
     */
    public boolean removeInst(Instruction inst) {
        if (!this.instructions.contains(inst)) {
            inst.removeSelf();
            return true;
        }
        else {
            throw new RuntimeException("Try to remove an Instruction that doesn't reside on the BasicBlock.");
        }
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

    @Override
    public Iterator<Instruction> iterator() {
        return instructions.iterator();
    }
}
