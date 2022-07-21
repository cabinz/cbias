package backend.armCode;

import backend.armCode.MCInstructions.MCload;
import backend.operand.Immediate;
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
     * Total stackSize, including local variables & spilled nodes. <br/>
     * stackSize = localVariable + spilledNode*4; <br/>
     * Function stack (from high to low): parameter, context, spilled nodes, local variables
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
    private int localVariable;
    /**
     * This field is used to record the number of
     * spilled virtual registers.
     */
    private int spilledNode;
    /**
     * This set holds all the load related to the
     * function's parameter address, <br/>
     * which need to be adjusted after {@link passes.mc.RegisterAllocation.GraphColoring}.
     */
    private HashSet<MCload> paramCal;

    public boolean useLR;
    private final boolean isExternal;

    /**
     * Represent the map between IR basic block and machine basic block
     */
    private final HashMap<BasicBlock, MCBasicBlock> BBmap;
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

    /**
     * Create a virtual register for some instruction
     * in the function
     */
    public VirtualRegister createVirReg(Value value){
        var vr = new VirtualRegister(VirtualRegCounter++, value);
        VirtualRegisters.add(vr);
        return vr;
    }

    /**
     * Create a virtual register for some instruction
     * in the function
     */
    public VirtualRegister createVirReg(int value){
        var vr = new VirtualRegister(VirtualRegCounter++, value);
        VirtualRegisters.add(vr);
        return vr;
    }

    public void addLocalVariable(int i) {localVariable += i;}

    /**
     * Add a register to context, meaning it was used in the function
     * @param index the index of the {@link RealRegister}
     */
    public void addContext(int index) {
        /* r0-r3 are caller-saved registers */
        /* r4-r12, lr are callee-saved */
        if (index > 3 && index != 13 && index != 15)
            context.add(RealRegister.get(index));
    }

    public void addSpilledNode() {spilledNode++;}

    /**
     * Add a parameter load instruction into function
     */
    public void addParamCal(MCload move) {paramCal.add(move);}

    /**
     * Get total stackSize, including local variables & spilled nodes. <br/>
     * stackSize = localVariable + spilledNode*4
     */
    public int getStackSize() {
        stackSize = localVariable + spilledNode*4;
        return stackSize;
    }

    /**
     * Iterable implement
     * @return Iterator<MCBasicBlock>
     */
    public Iterator<MCBasicBlock> iterator(){return BasicBlockList.iterator();}

    //</editor-fold>

    public String emit() {
        return name;
    }

    //<editor-fold desc="Getter & Setter">
    public String getName() {return name;}

    public HashSet<RealRegister> getContext() {return context;}
    public Integer getLocalVariable() {return localVariable;}
    public int getSpilledNode() {return spilledNode;}
    public HashSet<MCload> getParamCal() {return paramCal;}

    public LinkedList<MCBasicBlock> getBasicBlockList() {return BasicBlockList;}
    public ArrayList<VirtualRegister> getVirtualRegisters() {return VirtualRegisters;}

    public void setUseLR() {
        context.add(RealRegister.get(14));
        useLR = true;
    }
    public boolean isExternal() {return isExternal;}
    //</editor-fold>


    //<editor-fold desc="Constructor">
    public MCFunction(String name, boolean isExternal) {
        this.name = name;
        stackSize = 0;
        context = new HashSet<>();
        localVariable = 0;
        spilledNode = 0;
        paramCal = new HashSet<>();
        BasicBlockList = new LinkedList<>();
        VirtualRegisters = new ArrayList<>();
        BBmap = new HashMap<>();
        this.isExternal = isExternal;
//        argList = new LinkedList<MCOperand>();
    }
    //</editor-fold>
}
