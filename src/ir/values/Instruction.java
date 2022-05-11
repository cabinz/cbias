package ir.values;

import ir.User;
import ir.Type;

/**
 * Instruction class is the base class for all the IR instructions.
 * Various kinds of instruction inherit it and are tagged with an
 * InstCategory in cat field for distinction.
 * <br>
 * Type for an Instruction depends on its category.
 * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/Instruction.h">
 *     LLVM IR Reference</a>
 */
public class Instruction extends User {

    /**
     * Each instruction instance has a field "cat" containing a InstCategory instance
     * as a tag distinguishing different instruction types.
     * <br>
     * NOTICE: The ordinal of each enum will be use for distinguishing different types
     * of operations. Be careful to move them or add new ones.
     */
    public enum InstCategory {
        // Operations
        ADD, SUB, MUL, DIV,     // Arithmetic Operations
        LT, GT, EQ, NE, LE, GE, // Relationship (Logical) Operations
        AND, OR,                // Bitwise Operations
        // Terminators
        RET, BR, CALL,
        // Memory operations
        ZEXT, ALLOCA, LOAD, STORE
    }


    //<editor-fold desc="Fields">
    /**
     * An InstCategory instance indicating an instruction type.
     */
    public InstCategory cat;

//    /**
//     * Reference of the basic block where the instruction lands.
//     */
//    private BasicBlock bb;
    //</editor-fold>


    //<editor-fold desc="Constructors">
    public Instruction(Type type, InstCategory tag, int numOperands){
        super(type, numOperands);
        this.cat = tag;
    }
    //</editor-fold>


    //<editor-fold desc="Methods">
//    public BasicBlock getBB() {
//        return this.bb.getParent().getVal();
//    }

    public boolean isArithmeticBinary() {
        return this.cat.ordinal() <= InstCategory.DIV.ordinal();
    }

    public boolean isLogicalBinary() {
        return InstCategory.LT.ordinal() <= this.cat.ordinal()
                && this.cat.ordinal() <= InstCategory.GE.ordinal();
    }
    //</editor-fold>

}
