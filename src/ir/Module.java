package ir;

import java.util.ArrayList;

import ir.values.Function;

/**
 * The top-level class for a single IR file (compile unit).
 * WILL contain functions, global variables, symbol tables entries and other
 * resources needed.
 */

public class Module{
    //<editor-fold desc="Fields">
    public final ArrayList<Function> functions = new ArrayList<>();
    //</editor-fold>
}
