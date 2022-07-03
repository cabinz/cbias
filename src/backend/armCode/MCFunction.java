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

    public boolean useLR;
    private boolean isExternal;

    /**
     * Represent the map between IR basic block and machine basic block
     */
    private HashMap<BasicBlock, MCBasicBlock> BBmap;
//    private LinkedList<MCOperand> argList;
    //</editor-fold>


    //<editor-fold desc="Useful methods">

    /**
     * Append at the end of the BasicBlock list to a function.
     * @param IRBB the BasicBlock to be appended
     */
    public MCBasicBlock createBB(BasicBlock IRBB) {
        MCBasicBlock MCBB = new MCBasicBlock(this);
        BasicBlockList.add(MCBB);
        BBmap.put(IRBB, MCBB);
        return MCBB;
    }

    /**
     * Find the corresponding MC BasicBlock of an IR BasicBlock<br/>
     * (If not exited, create one)
     * @param IRBB the IR BasicBlock to search
     * @return the corresponding MC BasicBlock to find
     */
    public MCBasicBlock findMCBB(BasicBlock IRBB) {
        MCBasicBlock MCBB = BBmap.get(IRBB);
        return MCBB==null ?createBB(IRBB) :MCBB;
    }

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

    public boolean isExternal() {return isExternal;}
    //</editor-fold>


    //<editor-fold desc="Constructor">
    public MCFunction(String name, boolean isExternal) {
        this.name = name;
        stackSize = 0;
        BasicBlockList = new LinkedList<>();
        BBmap = new HashMap<>();
        this.isExternal = isExternal;
//        argList = new LinkedList<MCOperand>();
    }
    //</editor-fold>
}
