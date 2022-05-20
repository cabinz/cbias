package backend;

import backend.armCode.MCFunction;
import ir.values.Function;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * This class represent an object file to be emitted.
 */
public class ARMAssemble {

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
        var MCf = new MCFunction(IRf.name, false);
        functionList.add(MCf);
        functionMap.put(IRf, MCf);
        return MCf;
    }

    /**
     * Import external function. <br/>
     * used by BL external for a unified style
     * @param IRf
     */
    public void useExternalFunction(Function IRf){
        var MCf = new MCFunction(IRf.name, true);
        functionList.add(MCf);
        functionMap.put(IRf, MCf);
    }

    /**
     * Find the corresponding MC Function of an IR Function
     * @param IRf the IR Function to search
     * @return the corresponding MC Function to find
     */
    public MCFunction findMapFunc(Function IRf) {return functionMap.get(IRf);}

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
