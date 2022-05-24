package ir;

/**
 * A Use represents the edge between a Value definition and its users.
 * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/Use.h">
 *     LLVM IR Reference</a>
 */
public class Use {
    //<editor-fold desc="Fields">
    /**
     * Reference to value being used.
     */
    private Value v;

    /**
     * Reference to the user value.
     */
    private User u;

    /**
     * The position number of the value as an operand of the user.
     */
    private int pos;
    //</editor-fold>


    //<editor-fold desc="Constructors">

    /**
     * Construct a new Use edge.
     * <br>
     * The constructor automatically inserts the user reference to usee's uses field,
     * and the usee reference to user's operands field.
     * <br>
     * The insertions of references DO NOT check the replicates in the list containers,
     * correctness should be guaranteed by programmer.
     * @param value The value being used.
     * @param user  The user value.
     * @param position The position of the used value as an operand.
     */
    public Use(Value value, User user, int position) {
        this.v = value;
        this.u = user;
        this.pos = position;
        v.addUse(this);
        u.operands.add(this);
    }
    //</editor-fold>


    //<editor-fold desc="Methods">
    public int getOperandPos() {
        return pos;
    }

    public Value getValue() {
        return v;
    }

    public void setValue(Value v) {
        this.v = v;
    }
    //</editor-fold>
}
