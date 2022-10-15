package passes.ir.gcm;

import passes.ir.analysis.ILoopAnalysis;
import passes.ir.analysis.LoopAnalysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * BasicBlock with extra information.
 */
class BasicBlock extends passes.ir.BasicBlock implements ILoopAnalysis<BasicBlock> {

    public BasicBlock(ir.values.BasicBlock rawBasicBlock) {
        super(rawBasicBlock);
    }

    /// CFG

    private final Set<BasicBlock> entrySet = new HashSet<>();
    private final Set<BasicBlock> exitSet = new HashSet<>();

    @Override
    public void addEntryBlock(BasicBlock entryBlock) {
        entrySet.add(entryBlock);
    }

    @Override
    public void setExitBlocks(List<BasicBlock> exitBlocks) {
        exitSet.clear();
        exitSet.addAll(exitBlocks);
    }

    /// DOM tree

    private BasicBlock domFather = null;
    private final Set<BasicBlock> domSons = new HashSet<>();

    private Integer domDepth;

    private final ArrayList<BasicBlock> fastJumps = new ArrayList<>();

    private final ArrayList<BasicBlock> shallowestBBs = new ArrayList<>();

    public ArrayList<BasicBlock> getFastJumps() {
        return fastJumps;
    }

    public ArrayList<BasicBlock> getShallowestBBs() {
        return shallowestBBs;
    }


    public int getDomDepth() {
        return domDepth;
    }

    public void setDomDepth(int domDepth) {
        this.domDepth = domDepth;
    }

    private LoopAnalysis<BasicBlock>.Loop loop;

    public int getLoopDepth(){
        if(loop==null) return 0;
        else return loop.getDepth();
    }

    public LoopAnalysis<BasicBlock>.Loop getLoop() {
        return loop;
    }

    public void setLoop(LoopAnalysis<BasicBlock>.Loop loop) {
        this.loop = loop;
    }

    @Override
    public Set<BasicBlock> getEntryBlocks() {
        return entrySet;
    }

    @Override
    public Set<BasicBlock> getExitBlocks() {
        return exitSet;
    }

    @Override
    public void setDomFather(BasicBlock father) {
        domFather = father;
    }

    @Override
    public void addDomSon(BasicBlock son) {
        domSons.add(son);
    }

    public BasicBlock getDomFather() {
        return domFather;
    }

    public Set<BasicBlock> getDomSons() {
        return domSons;
    }

    /// Instruction reschedule

    private final Set<Instruction> pendingInstructions = new HashSet<>();

    public void addPendingInstructions(Instruction instruction){
        pendingInstructions.add(instruction);
    }

    public Set<Instruction> getPendingInstructions(){
        return pendingInstructions;
    }

}
