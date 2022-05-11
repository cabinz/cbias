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

public class Value {

    //<editor-fold desc="Fields">
    public Type type;

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
    public String name = "";

    /**
     * The "use list" keeping track of Values using it.
     */
    public LinkedList<Use> uses = new LinkedList<>();
    //</editor-fold>


    //<editor-fold desc="Constructors">
    public Value(Type type) {
        this.type = type;
    }
    //</editor-fold>
}
