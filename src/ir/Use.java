package ir;

/**
 * A Use represents the edge between a Value definition and its users.
 * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/Use.h">
 *     LLVM IR Reference</a>
 */
public class Use {
    /**
     * Reference to value being used.
     */
    private Value usee;

    public Value getUsee() {
        return usee;
    }

    public void setUsee(Value usee) {
        this.usee.removeUse(this);
        this.usee = usee;
        this.usee.addUse(this);
    }

    /**
     * Reference to the user value.
     */
    private User user;

    public User getUser() {
        return user;
    }

    /**
     * The position number of the value as an operand of the user.
     */
    private int pos;

    public int getOperandPos() {
        return pos;
    }

    /**
     * Construct a new Use edge.
     * <br>
     * The constructor DOES NOT automatically insert the Use link constructed to neither usee's [uses]
     * field nor the user's [operands] field, which are left for the caller to manually handle.
     * <br>
     * The insertions of references DO NOT check the replicates in the list containers,
     * correctness should be guaranteed by programmer.
     * @param value The value being used.
     * @param user  The user value.
     * @param position The position of the used value as an operand.
     */
    public Use(Value value, User user, int position) {
        this.usee = value;
        this.user = user;
        this.pos = position;
    }

    /**
     * Remove the Use link from its user and usee.
     * Notice that the Use itself will not be destructed.
     */
    public void removeSelf() {
        this.getUsee().removeUse(this);
        this.getUser().removeOperandAt(this.getOperandPos());
    }

    public User getUser(){
        return u;
    }

}
