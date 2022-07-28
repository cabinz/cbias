package ir.values;

import ir.Use;
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

    /**
     * All the Instructions in the BB.
     */
    public final LinkedList<Instruction> instructions = new LinkedList<>();
    // TODO: private the field, add a getter for it

    /**
     * Reference of the Function where the BasicBlock lands.
     * Null if the BB has never been inserted to any Function.
     */
    private Function func = null;

    public Function getFunc() {
        return func;
    }

    public void setFunc(Function func) {
        this.func = func;
    }


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
     * Remove the BB from the Function holding it.
     */
    public void removeSelf() {
        this.getFunc().removeBB(this);
    }

    /**
     * Removes a specified instruction from the BasicBlock.
     * All the related Use links will be simultaneously wiped out.
     * If the given Inst doesn't exit in the BB, an exception will be thrown.
     * @param inst The Instruction to be removed.
     */
    public void removeInst(Instruction inst) {
        if (this.instructions.contains(inst)) {
            // Update the use states:
            // - Remove all the Use links for the User using it.
            // - Remove all the Use links corresponding to its operands.
            @SuppressWarnings("unchecked")
            LinkedList<Use> tmpUses = (LinkedList<Use>) inst.getUses().clone();
            tmpUses.forEach(Use::removeSelf);
            inst.getOperands().forEach(Use::removeSelf);
            // Remove the inst from the bb.
            instructions.remove(inst);
            inst.setBB(null);
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
     * If the Inst has already belonged to another BB, an exception will be thrown.
     * @param inst The instruction to be inserted.
     */
    public void insertAtEnd(Instruction inst) {
        // Security checks.
        if (inst.getBB() != null) {
            throw new RuntimeException("Try to insert an Inst that has already belonged to another BB.");
        }
        if (this.instructions.size() != 0 && this.getLastInst().cat.isTerminator()) {
            throw new RuntimeException("Try to insert an Inst to a BB which has already ended with a Terminator.");
        }

        // Insertion.
        inst.setBB(this);
        this.instructions.addLast(inst);
    }

    /**
     * Insert an instruction at the beginning of the basic block.
     * If the Inst has already belonged to another BB, an exception will be thrown.
     * @param inst The instruction to be inserted.
     */
    public void insertAtFront(Instruction inst) {
        if (inst.getBB() != null) {
            throw new RuntimeException("Try to insert an Inst that has already belonged to another BB.");
        }
        inst.setBB(this);
        this.instructions.addFirst(inst);
    }

    @Override
    public Iterator<Instruction> iterator() {
        return instructions.iterator();
    }

    /**
     * Get LabelType for a BB.
     * @return A LabelType.
     */
    @Override
    public LabelType getType() {
        return (LabelType) super.getType();
    }
}
