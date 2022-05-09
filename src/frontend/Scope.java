package frontend;

import ir.Value;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Each module has an instance of Scope, maintaining the symbol tables for nested
 * code blocks in the source, and other semantic info of that environment
 * for looking up.
 */
public class Scope {

    //<editor-fold desc="Fields">
    /**
     * A list as a stack containing all symbol tables.
     * The top element is the symbol table of the current scope.
     */
    private final ArrayList<HashMap<String, Value>> tables = new ArrayList<>();

//    public boolean preEnter = false;
    //</editor-fold>

    //<editor-fold desc="Constructors">
    public Scope() {
        // Push a first symbol table for the module as initialization.
        tables.add(new HashMap<>());
    }
    //</editor-fold>

    //<editor-fold desc="Methods">
    /**
     * Push a new symbol table onto the stack when scoping.
     */
    public void scopeIn() {
        tables.add(new HashMap<>());
    }

    /**
     * Pop out the top symbol table from the stack when current scope exits.
     */
    public void scopeOut() {
        tables.remove(tables.size() - 1);
    }

    /**
     * Peek the top of the symbol table stack.
     * @return The symbol table of current scope.
     */
    public HashMap<String, Value> curTab() {
        return tables.get(tables.size() - 1);
    }

    /**
     * Add a new name-value pair into the symbol table of current scope.
     * @param name The name of the pair.
     * @param val The value of the pair.
     */
    public void addDecl(String name, Value val) {
        // Check name repetition.
        if(curTab().get(name) != null) {
            throw new RuntimeException("The name has been taken.");
        }
        // If it's a new name.
        else {
            curTab().put(name, val);
        }
    }

    /**
     * Find the object with the given name that can be used in current scope.
     * @param name The name to be used for searching.
     * @return The object with the name. Return null if no matched object is found.
     */
    public Value getValByName(String name) {
        // Search the object from current layer of scope to the outer ones.
        for (int i = tables.size() - 1; i >= 0; i--) {
            Value val = tables.get(i).get(name);
            if(val != null)
                return val;
        }
        return null;
    }

    //</editor-fold>


}
