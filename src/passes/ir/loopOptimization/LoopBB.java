package passes.ir.loopOptimization;

import passes.ir.analysis.ILoopAnalysis;
import passes.ir.analysis.LoopAnalysis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LoopBB extends passes.ir.BasicBlock implements ILoopAnalysis<LoopBB> {

    //<editor-fold desc="Doms">
    private LoopBB domFather = null;
    private final Set<LoopBB> domSons = new HashSet<>();
    private Integer domDepth;

    @Override
    public void setDomFather(LoopBB father) {domFather = father;}

    @Override
    public void addDomSon(LoopBB son) {domSons.add(son);}

    @Override
    public LoopBB getDomFather() {return domFather;}

    @Override
    public Set<LoopBB> getDomSons() {return domSons;}

    @Override
    public void setDomDepth(int depth) {domDepth = depth;}

    @Override
    public int getDomDepth() {return domDepth;}
    //</editor-fold>

    //<editor-fold desc="Loops">
    private LoopAnalysis<LoopBB>.Loop loop;

    @Override
    public void setLoop(LoopAnalysis<LoopBB>.Loop loop) {this.loop = loop;}

    @Override
    public LoopAnalysis<LoopBB>.Loop getLoop() {return loop;}
    //</editor-fold>

    //<editor-fold desc="Relations">
    private final Set<LoopBB> entrySet = new HashSet<>();
    private final Set<LoopBB> exitSet = new HashSet<>();

    @Override
    public void addEntryBlock(LoopBB entryBlock) {
        entrySet.add(entryBlock);
    }

    @Override
    public void setExitBlocks(List<LoopBB> exitBlocks) {
        exitSet.clear();
        exitSet.addAll(exitBlocks);
    }

    @Override
    public Set<LoopBB> getEntryBlocks() {
        return entrySet;
    }

    @Override
    public Set<LoopBB> getExitBlocks() {
        return exitSet;
    }
    //</editor-fold>


    public LoopBB(ir.values.BasicBlock rawBasicBlock) {super(rawBasicBlock);}
}
