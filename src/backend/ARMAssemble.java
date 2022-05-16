package backend;

import backend.armCode.MCFunction;

import java.util.LinkedList;

/**
 * This class represent an object file to be emitted.
 */
public class ARMAssemble {

    //<editor-fold desc="Fields">
    private String architecture = "armv7";
    private LinkedList<MCFunction> functionList;
    //</editor-fold>

    public MCFunction createFunction(String name){
        var function = new MCFunction(name);
        functionList.add(function);
        return function;
    }

    //<editor-fold desc="Getter & Setter">
    public String getArchitecture() {return architecture;}

    public LinkedList<MCFunction> getFunctionList() {return functionList;}
    //</editor-fold>

    //<editor-fold desc="Constructor">
    public ARMAssemble(){
        functionList = new LinkedList<>();
    }
    //</editor-fold>
}
