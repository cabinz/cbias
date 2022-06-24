package backend;

import backend.armCode.MCFunction;
import ir.values.Function;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * This class represent an object file to be emitted.
 */
public class ARMAssemble implements Iterable<MCFunction>{

    //<editor-fold desc="Fields">
    private String architecture = "armv7";
    private LinkedList<MCFunction> functionList;

    private HashMap<Function, MCFunction> functionMap;
    //</editor-fold>

    /**
     * Create a Function in the assembly while return the corresponding MC Function.
     * @param IRf the IR Function to be created
     * @return the corresponding MC Function
     */
    public MCFunction createFunction(Function IRf){
        var MCf = new MCFunction(IRf.getName(), false);
        functionList.add(MCf);
        functionMap.put(IRf, MCf);
        return MCf;
    }

    /**
     * Import external function. <br/>
     * used by BL external for a unified style
     * @param IRFunc the external IR Function to be used
     */
    public void useExternalFunction(Function IRFunc){
        var MCFunc = new MCFunction(IRFunc.getName(), true);
        functionList.add(MCFunc);
        functionMap.put(IRFunc, MCFunc);
    }

    /**
     * Find the corresponding MC Function of an IR Function
     * @param IRFunc the IR Function to search
     * @return the corresponding MC Function to find
     */
    public MCFunction findMCFunc(Function IRFunc) {return functionMap.get(IRFunc);}

    public Iterator<MCFunction> iterator(){return functionList.iterator();}

    //<editor-fold desc="Getter & Setter">
    public String getArchitecture() {return architecture;}

    public LinkedList<MCFunction> getFunctionList() {return functionList;}
    //</editor-fold>

    //<editor-fold desc="Constructor">
    public ARMAssemble(){
        functionList = new LinkedList<>();
        functionMap = new HashMap<>();
    }
    //</editor-fold>
}
