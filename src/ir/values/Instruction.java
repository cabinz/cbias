package ir.values;

import ir.User;
import ir.Type;

/**
 * Instruction class is the base class for all the IR instructions
 * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/Instruction.h">
 *     LLVM IR Reference</a>
 */
public class Instruction extends User {

    /**
     * Each instruction instance has a field "cat" containing a InstCategory instance
     * as a tag distinguishing different instruction types.
     */
    public enum InstCategory {
        // Operations
        ADD, SUB, MUL, DIV,     // Arithmetic Operators
        LT, GT, EQ, NE, LE, GE, // Relationship (Logical) Operators
        AND, OR,                // Bitwise Operators
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
    //</editor-fold>

}
