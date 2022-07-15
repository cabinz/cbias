package backend.armCode;

import backend.operand.RealRegister;
import backend.operand.VirtualRegister;
import ir.Value;
import ir.values.BasicBlock;

import java.util.*;

/**
 * This class represents a function of ARM in memory.
 */
public class MCFunction implements Iterable<MCBasicBlock> {

    //<editor-fold desc="Fields">
    private final LinkedList<MCBasicBlock> BasicBlockList;
    /**
     * This is used to name the virtual register.
     */
    private int VirtualRegCounter = 0;
    private final ArrayList<VirtualRegister> VirtualRegisters;
    private final String name;

    /**
     * Total stackSize, including context, local variables & spilled nodes. <br/>
     * stackSize = context *4 + sum(localVariable) + spilledNode*4;
     * Function stack (from high to low): parameter, context, local variables, spilled nodes
     */
    private int stackSize;
    /**
     * This field is used to record the number of
     * callee-saved registers that need to be saved.
     */
    private HashSet<RealRegister> context;
    /**
     * This field is used to record the sizes of
     * local variables.
     */
    private ArrayList<Integer> localVariable;
    /**
     * This field is used to record the number of
     * spilled virtual registers.
     */
    private int spilledNode;

    public boolean useLR;
    private final boolean isExternal;

    /**
     * Represent the map between IR basic block and machine basic block
     */
    private final HashMap<BasicBlock, MCBasicBlock> BBmap;
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

    public MCBasicBlock getEntryBlock() {
        return BasicBlockList.getFirst();
    }

    public VirtualRegister createVirReg(Value value){
        var vr = new VirtualRegister(VirtualRegCounter++, value);
        VirtualRegisters.add(vr);
        return vr;
    }

    public VirtualRegister createVirReg(int value){
        var vr = new VirtualRegister(VirtualRegCounter++, value);
        VirtualRegisters.add(vr);
        return vr;
    }

    public void addLocalVariable(int i) {localVariable.add(i);}

    public void addContext(int index) {
        /* r0-r3 are caller-saved registers */
        /* r4-r12, lr are callee-saved */
        if (index > 3 && index != 13 && index != 15)
            context.add(RealRegister.get(index));
    }

    public void addSpilledNode() {spilledNode++;}

    public int getStackSize() {
        stackSize = context.size()*4 + localVariable.stream().mapToInt(v ->v).sum() + spilledNode*4;
        return stackSize;
    }

    /**
     * Iterable implement
     * @return Iterator<MCBasicBlock>
     */
    public Iterator<MCBasicBlock> iterator(){return BasicBlockList.iterator();}

    //</editor-fold>

    //<editor-fold desc="Getter & Setter">
    public String getName() {return name;}

    public HashSet<RealRegister> getContext() {return context;}
    public ArrayList<Integer> getLocalVariable() {return localVariable;}
    public int getSpilledNode() {return spilledNode;}

    public LinkedList<MCBasicBlock> getBasicBlockList() {return BasicBlockList;}
    public ArrayList<VirtualRegister> getVirtualRegisters() {return VirtualRegisters;}

    public boolean isExternal() {return isExternal;}
    //</editor-fold>


    //<editor-fold desc="Constructor">
    public MCFunction(String name, boolean isExternal) {
        this.name = name;
        stackSize = 0;
        context = new HashSet<>();
        localVariable = new ArrayList<>();
        spilledNode = 0;
        BasicBlockList = new LinkedList<>();
        VirtualRegisters = new ArrayList<>();
        BBmap = new HashMap<>();
        this.isExternal = isExternal;
//        argList = new LinkedList<MCOperand>();
    }
    //</editor-fold>
}
