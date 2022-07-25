package backend.armCode;

import backend.armCode.MCInstructions.MCFPload;
import backend.armCode.MCInstructions.MCload;
import backend.operand.RealRegister;
import backend.operand.VirtualExtRegister;
import backend.operand.VirtualRegister;
import ir.Value;
import ir.values.BasicBlock;
import ir.values.Function;

import java.util.*;

/**
 * This class represents a function of ARM in memory.
 */
public class MCFunction implements Iterable<MCBasicBlock> {

    //<editor-fold desc="Fields">

    //<editor-fold desc="Basic info">
    private final LinkedList<MCBasicBlock> BasicBlockList;
    private final Function IRFunction;
    /* Arguments passed via core register */
    private final ArrayList<Function.FuncArg> APVCR;
    /* Arguments passed via extension register */
    private final ArrayList<Function.FuncArg> APVER;
    /* Arguments copied to memory */
    private final ArrayList<Function.FuncArg> ACTM;
    /* 起名真的太难了 */
    //</editor-fold>

    //<editor-fold desc="Registers">
    /**
     * This is used to name the virtual register.
     */
    private int VirtualRegCounter = 0;
    private final ArrayList<VirtualRegister> VirtualRegisters;

    private int virtualExtRegCounter = 0;
    private final ArrayList<VirtualExtRegister> virtualExtRegisters;
    //</editor-fold>

    //<editor-fold desc="Stack frame info">
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
    private HashSet<MCFPload> floatParamLoads;
    //</editor-fold>

    //<editor-fold desc="Other info">
    public boolean useLR;
    private final boolean isExternal;

    /**
     * Represent the map between IR basic block and machine basic block
     */
    private final HashMap<BasicBlock, MCBasicBlock> BBmap;
    //</editor-fold>
    //</editor-fold>


    //<editor-fold desc="Useful methods">

    //<editor-fold desc="Basic">
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

    public void paramAnalysis() {
        var args = IRFunction.getArgs();
        int argNum = args.size();
        /* Next Core Register Number, @see AAPCS */
        int NCRN = 0;
        /* Next Extension Register Number */
        int NERN = 0;

        /* Assignment of arguments to registers and stack */
        for (int i=0; i<argNum; i++) {
            var param = args.get(i);
            if (param.getType().isIntegerType() || param.getType().isPointerType()) {
                if (NCRN < 4) {
                    APVCR.add(param);
                    NCRN++;
                }
                else
                    ACTM.add(param);
            }
            else if (param.getType().isFloatType()) {
                if (NERN < 16) {
                    APVER.add(param);
                    NERN++;
                }
                else
                    ACTM.add(param);
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="Register related">
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

    public VirtualExtRegister createExtVirReg(Value value) {
        var vr = new VirtualExtRegister(virtualExtRegCounter++, value);
        virtualExtRegisters.add(vr);
        return vr;
    }
    //</editor-fold>

    //<editor-fold desc="Stack frame related">
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

    public void addFloatParamLoads(MCFPload load) {floatParamLoads.add(load);}

    /**
     * Get total stackSize, including local variables & spilled nodes. <br/>
     * stackSize = localVariable + spilledNode*4
     */
    public int getStackSize() {
        stackSize = localVariable + spilledNode*4;
        return stackSize;
    }
    //</editor-fold>

    /**
     * Iterable implement
     * @return Iterator<MCBasicBlock>
     */
    public Iterator<MCBasicBlock> iterator(){return BasicBlockList.iterator();}

    //</editor-fold>

    public String emit() {
        return IRFunction.getName();
    }

    //<editor-fold desc="Getter & Setter">
    public String getName() {return IRFunction.getName();}
    public ArrayList<Function.FuncArg> getAPVCR() {return APVCR;}
    public ArrayList<Function.FuncArg> getAPVER() {return APVER;}
    public ArrayList<Function.FuncArg> getACTM() {return ACTM;}

    public HashSet<RealRegister> getContext() {return context;}
    public Integer getLocalVariable() {return localVariable;}
    public int getSpilledNode() {return spilledNode;}
    public HashSet<MCload> getParamCal() {return paramCal;}
    public HashSet<MCFPload> getFloatParamLoads() {return floatParamLoads;}

    public LinkedList<MCBasicBlock> getBasicBlockList() {return BasicBlockList;}
    public ArrayList<VirtualRegister> getVirtualRegisters() {return VirtualRegisters;}
    public ArrayList<VirtualExtRegister> getVirtualExtRegisters() {return virtualExtRegisters;}

    public void setUseLR() {
        context.add(RealRegister.get(14));
        useLR = true;
    }
    public boolean isExternal() {return isExternal;}
    //</editor-fold>


    //<editor-fold desc="Constructor">
    public MCFunction(Function IRFunction, boolean isExternal) {
        this.IRFunction = IRFunction;
        stackSize = 0;
        context = new HashSet<>();
        localVariable = 0;
        spilledNode = 0;
        paramCal = new HashSet<>();
        floatParamLoads = new HashSet<>();
        BasicBlockList = new LinkedList<>();
        ACTM = new ArrayList<>();
        APVCR = new ArrayList<>();
        APVER = new ArrayList<>();
        VirtualRegisters = new ArrayList<>();
        virtualExtRegisters = new ArrayList<>();
        BBmap = new HashMap<>();
        this.isExternal = isExternal;
        paramAnalysis();
//        argList = new LinkedList<MCOperand>();
    }
    //</editor-fold>
}
