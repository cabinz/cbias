package backend.armCode;

import ir.values.BasicBlock;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * This class represents a function of ARM in memory.
 */
public class MCFunction {

    //<editor-fold desc="Fields">
    private LinkedList<MCBasicBlock> BasicBlockList;
    private String name;

    private int stackSize;
    private boolean useLR;

    /**
     * Represent the map between IR basic block and machine basic block
     */
    private HashMap<BasicBlock, MCBasicBlock> BBmap;
//    private LinkedList<MCOperand> argList;
    //</editor-fold>


    //<editor-fold desc="Useful methods">

    /**
     * Append at the end of the BasicBlock list to a function.
     * @param BasicBlock the BasicBlock to be appended
     */
    public void appendBB(MCBasicBlock BasicBlock) {BasicBlockList.add(BasicBlock);}


    /**
     * Used when declare a local variable
     * @param n the size of the variable
     */
    public void addStackSize(int n) {stackSize += n;}


    //</editor-fold>

    //<editor-fold desc="Getter & Setter">
    public String getName() {return name;}
    public void setName(String name) {this.name = name;}

    public int getStackSize() {return stackSize;}
    //</editor-fold>


    //<editor-fold desc="Constructor">
    public MCFunction(String name) {
        this.name = name;
        stackSize = 0;
        BasicBlockList = new LinkedList<MCBasicBlock>();
//        argList = new LinkedList<MCOperand>();
    }
    //</editor-fold>
}
