package ir;

import java.util.HashMap;
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
     * Keep track of all the Values used by a (pos -> val) mapping.
     * <br>
     * To safely add operands to it, use addOperandAt().
     */
    private final HashMap<Integer, Use> operands = new HashMap<>();


    public User(Type type) {
        super(type);
    }


    /**
     * Retrieve the number of Value it uses.
     * @return Number of operands of the user.
     */
    public int getNumOperands() {
        return operands.size();
    }

    /**
     * Retrieve a LinkedList containing all Uses for operands used.
     * (The LinkedList does NOT guarantee the order of the operands,
     * i.e. the index of each element in the LinkedList may not be
     * the position of the operand)
     * @return A LinkedList of all Uses of the operands.
     */
    public LinkedList<Use> getOperands() {
        return new LinkedList<>(operands.values());
    }

    /**
     * Retrieve a value used at a specified position.
     * @param pos The position of the target operands.
     * @return Reference of the target operand at the given position. Null if it doesn't exist.
     */
    public Value getOperandAt(int pos) {
        if (!operands.containsKey(pos)) {
            throw new RuntimeException("Operand index (position) doesn't exist.");
        }
        return operands.get(pos).getUsee();
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
        if (operands.containsKey(pos)) {
            throw new RuntimeException("Try to add an operand at an occupied position.");
        }
        // If not, add the given Value as a new use.
        var use = new Use(val, this, pos);
        this.operands.put(pos, use);
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
        if (operands.containsKey(pos)) {
            operands.get(pos).setUsee(val);
        }
        // If not, throw an exception.
        else {
            throw new RuntimeException("Try to reassign a non-existent operand.");
        }
    }

    /**
     * Remove an operand at the specified position
     * If there's no existing operand matched, an Exception will be thrown.
     * <br>
     * NOTICE: This is a unilateral removal. To safely delete a
     * user-usee relation, use Use::removeSelf instead.
     * @param pos Given operand position.
     */
    public void removeOperandAt(int pos) {
        if (operands.remove(pos) == null) { // from User
            throw new RuntimeException("Try to remove a non-existent operand.");
        }
    }
}
