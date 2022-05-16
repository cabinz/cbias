package backend.armCode;

import ir.values.BasicBlock;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * This class represents a function of ARM in memory.
 */
public class MCFunction  implements Iterable<MCBasicBlock> {

    //<editor-fold desc="Fields">
    private LinkedList<MCBasicBlock> BasicBlockList;
    private final String name;

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
     * This method provides a way to build mapping between IR Basic Block and assemble Basic Block.
     * @param BB IR Basic Block
     * @param MCBB Assemble Basic Block
     */
    public void addMap(BasicBlock BB, MCBasicBlock MCBB) {
        BBmap.put(BB, MCBB);
    }

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

    /**
     * Iterable implement
     * @return Iterator<MCBasicBlock>
     */
    public Iterator<MCBasicBlock> iterator(){return BasicBlockList.iterator();}

    //</editor-fold>

    //<editor-fold desc="Getter & Setter">
    public String getName() {return name;}

    public int getStackSize() {return stackSize;}

    public LinkedList<MCBasicBlock> getBasicBlockList() {return BasicBlockList;}
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
