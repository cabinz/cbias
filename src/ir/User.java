package ir;

import java.util.LinkedList;

/**
 * This class defines the interface that one who uses a Value must implement
 * to gain necessary extended fields for tracing all the Value instances used.
 * <br>
 * Each instance of the Value class keeps track of what User's have handles to it.
 * <ul>
 *     <li>Instructions are the largest class of Users.</li>
 *     <li>Constants may be users of other constants (think arrays and stuff)</li>
 * </ul>
 * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/User.h">
 *     LLVM IR Reference</a>
 */
public class User extends Value {

    //<editor-fold desc="Fields">
    /**
     * Keep track of all the Values used.
     * <br>
     * To safely add operands to it, use addOperandAt().
     */
    public LinkedList<Use> operands = new LinkedList<Use>(); // All the Use edges indicating to all the value used

    /**
     * Number of the Values the user use.
     */
    private int numOperands = 0;
    //</editor-fold>


    //<editor-fold desc="Constructors">
    public User(Type type) {
        super(type);
    }

    public User(Type type, int numOperand) {
        super(type);
        this.numOperands = numOperand;
    }
    //</editor-fold>



    //<editor-fold desc="Methods">
    public int getNumOperands() {
        return numOperands;
    }

    /**
     * Retrieve a value used at a specified position.
     * @param pos The position of the target operands.
     * @return Reference of the target operand at the given position. Null if it doesn't exist.
     */
    public Value getOperandAt(int pos) {
        for (Use use : operands) {
            if (use.getOperandPos() == pos) {
                return use.getValue();
            }
        }
        return null;
    }

    /**
     * At a new operand at a given position.
     * If an existed operand has already landed on that position, it will be safely replaced.
     * @param v The value to be added as an operand.
     * @param pos Given operand position.
     */
    public void addOperandAt(Value v, int pos) {
        // Check if there is an existing operand on the given position.
        for (Use use : operands) {
            // If there is, replace with the new Value.
            if (use.getOperandPos() == pos) {
                // Got the target use.
                use.getValue().removeUse(use); // Remove the original use.v (from the value using it).
                use.setValue(v); // Cover the use.v at specified position (pos) with given v.
                v.addUse(use); // Add the new use to the value using it (the given v).
                return;
            }
        }
        // If not, insert the given Value as an operand.
        new Use(v, this, pos);
    }
    //</editor-fold>
}
