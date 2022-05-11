package ir.values;

import ir.Value;
import ir.Type;

import java.util.ArrayList;

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
public class BasicBlock extends Value {

    //<editor-fold desc="Fields">
    public String name;
    private ArrayList<BasicBlock> predecessors;
    private ArrayList<BasicBlock> successors;
    public final ArrayList<Instruction> instructions = new ArrayList<>();
    //</editor-fold>

    //<editor-fold desc="Constructors">
    public BasicBlock(String name) {
        super(Type.LabelType.getType());

        this.name = name;
    }
    //</editor-fold>
}
