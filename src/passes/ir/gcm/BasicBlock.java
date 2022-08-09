package passes.ir.gcm;

import passes.ir.gcm.domtree.IDomTreeNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class BasicBlock extends passes.ir.BasicBlock implements IDomTreeNode<BasicBlock> {

    public BasicBlock(ir.values.BasicBlock rawBasicBlock) {
        super(rawBasicBlock);
    }

    /// CFG

    private final Set<BasicBlock> entrySet = new HashSet<>();
    private final Set<BasicBlock> exitSet = new HashSet<>();

    @Override
    public void addPreviousBasicBlock(BasicBlock previousBlock) {
        entrySet.add(previousBlock);
    }

    @Override
    public void setFollowingBasicBlocks(List<BasicBlock> followingBasicBlocks) {
        exitSet.clear();
        exitSet.addAll(followingBasicBlocks);
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


    public Integer getDomDepth() {
        return domDepth;
    }

    public void setDomDepth(Integer domDepth) {
        this.domDepth = domDepth;
    }

    private LoopAnalysis.Loop loop;

    public int getLoopDepth(){
        if(loop==null) return 0;
        else return loop.getDepth();
    }

    public LoopAnalysis.Loop getLoop() {
        return loop;
    }

    public void setLoop(LoopAnalysis.Loop loop) {
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

    private final List<Instruction> pendingInstructions = new ArrayList<>();

    public void addPendingInstructions(Instruction instruction){
        pendingInstructions.add(instruction);
    }

    public List<Instruction> getPendingInstructions(){
        return pendingInstructions;
    }

}
