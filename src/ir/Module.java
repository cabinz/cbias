package ir;

import java.util.ArrayList;
import java.util.LinkedList;

import ir.values.Function;
import ir.values.GlobalVariable;

/**
 * The top-level class for a single IR file (compile unit).
 * WILL contain functions, global variables, symbol tables entries and other
 * resources needed.
 */

public class Module {
    public final ArrayList<Function> functions = new ArrayList<>();

    public final ArrayList<Function> externFunctions = new ArrayList<>();

    public final LinkedList<GlobalVariable> globalVariables = new LinkedList<>();

    public void addGlbVar(GlobalVariable glbVar) {
        globalVariables.add(glbVar);
    }
}
