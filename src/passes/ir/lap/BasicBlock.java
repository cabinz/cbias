package passes.ir.lap;

import passes.ir.analysis.IDomAnalysis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * BasicBlock with extra information.
 */
public class BasicBlock extends passes.ir.BasicBlock implements IDomAnalysis<BasicBlock> {

    public BasicBlock(ir.values.BasicBlock rawBasicBlock) {
        super(rawBasicBlock);
    }

    /// DOM

    private BasicBlock domFather;
    private final Set<BasicBlock> domSons = new HashSet<>();
    private int domDepth, domDfn, domDfnR;

    @Override
    public void setDomFather(BasicBlock father) {
        this.domFather = father;
    }

    @Override
    public void addDomSon(BasicBlock son) {
        this.domSons.add(son);
    }

    @Override
    public BasicBlock getDomFather() {
        return domFather;
    }

    @Override
    public Set<BasicBlock> getDomSons() {
        return domSons;
    }

    @Override
    public void setDomDepth(int depth) {
        this.domDepth = depth;
    }

    @Override
    public int getDomDepth() {
        return domDepth;
    }

    public void setDomDfn(int domDfn) {
        this.domDfn = domDfn;
    }

    public int getDomDfn() {
        return domDfn;
    }

    public int getDomDfnL() {
        return getDomDfn();
    }

    public void setDomDfnR(int domDfnR) {
        this.domDfnR = domDfnR;
    }

    public int getDomDfnR() {
        return domDfnR;
    }

    /// CFG

    private final Set<BasicBlock> entryBlocks = new HashSet<>();
    private final Set<BasicBlock> exitBlocks = new HashSet<>();

    @Override
    public void addEntryBlock(BasicBlock entryBlock) {
        this.entryBlocks.add(entryBlock);
    }

    @Override
    public void setExitBlocks(List<BasicBlock> exitBlocks) {
        this.exitBlocks.clear();
        this.exitBlocks.addAll(exitBlocks);
    }

    @Override
    public Set<BasicBlock> getEntryBlocks() {
        return entryBlocks;
    }

    @Override
    public Set<BasicBlock> getExitBlocks() {
        return exitBlocks;
    }
}
