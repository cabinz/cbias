package ir;

import java.util.LinkedList;

/**
 * The ultimate base class in the (LLVM) IR value system.
 * It derives BasicBlock, Constant, and many other classes of IR structure classes
 * under the 'values' module.
 * <br>
 * Every value has a "use list" that keeps track of which other Values are using this Value.
 * <br>
 * All Values have a Type. In different derived class of Value, Type carries various forms
 * of type information of corresponding Value instances.
 * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/Value.h#L75">
 *     LLVM IR Source: Value</a>
 */

public abstract class Value {

    private final Type type;

    /**
     * All values can potentially be named.
     * The meaning of it depends on what derived Value it is.
     * e.g.
     * <ul>
     *     <li>for Function, name is its identifier</li>
     *     <li>for BasicBlock, name is its entry label</li>
     *     <li>for instruction yielding a result, name is the
     *     reference (register) to the result</li>
     * </ul>
     */
    private String name = "";

    /**
     * The "use list" keeping track of Values using it.
     */
    private LinkedList<Use> uses = new LinkedList<>();


    public Value(Type type) {
        this.type = type;
    }

    //<editor-fold desc="Getters">
    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public LinkedList<Use> getUses() {
        return uses;
    }
    //</editor-fold>

    //<editor-fold desc="Setters">
    public void setName(String name) {
        this.name = name;
    }
    //</editor-fold>

    /**
     * Add a Use to the use-list of the Value.
     * @param u The Use to be added.
     */
    public void addUse(Use u) {
        uses.add(u);
    }

    /**
     * Remove a Use from the use-list of the Value (IF EXISTS).
     * @param u The Use to be matched and removed.
     * @return Return true if any Use in the list has been removed. Otherwise, false.
     */
    public boolean removeUse(Use u) {
        return this.uses.removeIf(x -> x.equals(u));
    }
}
