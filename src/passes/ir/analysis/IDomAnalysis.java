package passes.ir.analysis;

import java.util.Set;

/**
 * BasicBlock interface for dom analysis.
 * @param <BasicBlock> Type where information are stored in.
 */
public interface IDomAnalysis<BasicBlock extends IDomAnalysis<BasicBlock>> extends IRelationAnalysis<BasicBlock> {
    void setDomFather(BasicBlock father);
    void addDomSon(BasicBlock son);

    BasicBlock getDomFather();

    Set<BasicBlock> getDomSons();
    void setDomDepth(int depth);

    int getDomDepth();

}
