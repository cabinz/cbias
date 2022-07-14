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
public abstract class User extends Value {

    /**
     * Keep track of all the Values used.
     * <br>
     * To safely add operands to it, use addOperandAt().
     */
    public LinkedList<Use> operands = new LinkedList<Use>();


    public User(Type type) {
        super(type);
    }


    /**
     * Retrieve the number of Value it use.
     * @return Number of operands of the user.
     */
    public int getNumOperands() {
        return operands.size();
    }

    /**
     * Retrieve a value used at a specified position.
     * @param pos The position of the target operands.
     * @return Reference of the target operand at the given position. Null if it doesn't exist.
     */
    public Value getOperandAt(int pos) {
        for (Use use : operands) {
            if (use.getOperandPos() == pos) {
                return use.getUsee();
            }
        }
        return null;
    }

    /**
     * At a new operand at a given position.
     * If an existed operand has already landed on that position, an Exception will be thrown.
     * @param val The value to be added as an operand.
     * @param pos Given operand position.
     */
    public void addOperandAt(Value val, int pos) {
        // Check if there is an existing operand on the given position.
        // If there is, throw an exception.
        for (Use use : operands) {
            // If there is, replace with the new Value.
            if (use.getOperandPos() == pos) {
                throw new RuntimeException("Try to add an operand at an occupied position.");
            }
        }
        // If not, add the given Value as a new use.
        var use = new Use(val, this, pos);
        this.operands.add(use);
        val.addUse(use);
    }

    /**
     * Set an operand at the specified position to be another Value given.
     * If there's no existing operand matched, an Exception will be thrown.
     * @param val The value to be set as an operand.
     * @param pos Given operand position.
     */
    public void setOperandAt(Value val, int pos) {
        // Check if there is the matched operand on the given position.
        // If there is, redirect it safely.
        for (Use use : operands) {
            // If there is, replace with the new Value.
            if (use.getOperandPos() == pos) {
                // Got the target use.
                use.setUsee(val); // Cover the use.v at specified position (pos) with given v.
                return;
            }
        }
        // If not, throw an exception.
        throw new RuntimeException("Try to reassign a non-existent operand.");
    }

    /**
     * Remove an operand at the specified position
     * If there's no existing operand matched, an Exception will be thrown.
     * @param pos Given operand position.
     */
    public void removeOperandAt(int pos) {
        for (Use use : operands) {
            // If there is, remove it.
            if (use.getOperandPos() == pos) {
                // Got the target use.
                this.operands.remove(use);
                return;
            }
        }
        // If not, throw an exception.
        throw new RuntimeException("Try to remove a non-existent operand.");
    }
}
