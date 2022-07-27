package backend;

import backend.armCode.MCFunction;
import backend.operand.Label;
import ir.types.ArrayType;
import ir.types.PointerType;
import ir.values.Constant;
import ir.values.Function;
import ir.values.GlobalVariable;
import ir.values.constants.ConstArray;
import ir.values.constants.ConstFloat;
import ir.values.constants.ConstInt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * This class represent an object file to be emitted.
 */
public class ARMAssemble implements Iterable<MCFunction>{

    //<editor-fold desc="Fields">
    public final String cpu = "cortex-a7";
    public final String architecture = "armv7ve";
    public final String fpu = "vfpv4";
    private final LinkedList<MCFunction> functionList;
    private final LinkedList<Label> globalVars;

    private final HashMap<Function, MCFunction> functionMap;
    private final HashMap<GlobalVariable, Label> glbVarMap;
    //</editor-fold>

    /**
     * Create a Function in the assembly while return the corresponding MC Function.
     * @param IRf the IR Function to be created
     * @return the corresponding MC Function
     */
    public MCFunction createFunction(Function IRf){
        var MCf = new MCFunction(IRf, false);
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
        var MCFunc = new MCFunction(IRFunc, true);
        functionList.add(MCFunc);
        functionMap.put(IRFunc, MCFunc);
    }

    /**
     * Find the corresponding MC Function of an IR Function
     * @param IRFunc the IR Function to search
     * @return the corresponding MC Function to find
     */
    public MCFunction findMCFunc(Function IRFunc) {return functionMap.get(IRFunc);}

    /**
     * Create a GlobalVariable in ARM for an
     * IR GlobalVariable, while return the corresponding label
     * @param gv the IR global variable
     * @return the corresponding label
     */
    public Label addGlobalVariable(GlobalVariable gv) {
        Label label;

        var type = ((PointerType) gv.getType()).getRootType();
        if (type.isIntegerType() || (type.isArrayType() && ((ArrayType) type).getPrimitiveType().isIntegerType())) {
            ArrayList<Integer> initial = new ArrayList<>();
            /* When global variable is not initialized, the getInitVal() will return null */
            if (gv.getInitVal() == null) {
                int size = ((ArrayType) gv.getConstType()).getSize();
                while ((size--) != 0) initial.add(0);
            }
            else
                genInitial(gv.getInitVal(), initial);

            /* 可恶的前端大佬，全局变量名字里带'@'，只能在这里消掉 */
            label = new Label(gv.getName().substring(1), Label.TAG.Int, initial);
        }
        else {
            ArrayList<Float> initial = new ArrayList<>();
            if (gv.getInitVal() == null) {
                int size = ((ArrayType) gv.getConstType()).getSize();
                while ((size--) != 0) initial.add(0.0f);
            }
            else
                genInitial(gv.getInitVal(), initial);

            /* 可恶的前端大佬，全局变量名字里带'@'，只能在这里消掉 */
            label = new Label(gv.getName().substring(1), Label.TAG.Float, initial);
        }

        globalVars.add(label);
        glbVarMap.put(gv, label);
        return label;
    }

    @SuppressWarnings("unchecked")
    private void genInitial(Constant constVals, ArrayList initial) {
            if (constVals.getType().isIntegerType())
                initial.add(((ConstInt) constVals).getVal());
            else if (constVals.getType().isFloatType())
                initial.add(((ConstFloat) constVals).getVal());
            else {
                ConstArray arr = ((ConstArray) constVals);
                for (int i=0; i<arr.getNumOperands(); i++)
                    genInitial(((Constant) arr.getOperandAt(i)), initial);
            }
    }

    /**
     * Find the corresponding data label
     * @param gv the IR global var to be search
     * @return the corresponding label
     */
    public Label findGlobalVar(GlobalVariable gv) {return glbVarMap.get(gv);}

    public Iterator<MCFunction> iterator(){return functionList.iterator();}

    //<editor-fold desc="Getter & Setter">
    public LinkedList<MCFunction> getFunctionList() {return functionList;}

    public LinkedList<Label> getGlobalVars() {return globalVars;}
    //</editor-fold>

    //<editor-fold desc="Constructor">
    public ARMAssemble(){
        functionList = new LinkedList<>();
        globalVars = new LinkedList<>();
        functionMap = new HashMap<>();
        glbVarMap = new HashMap<>();
    }
    //</editor-fold>
}
