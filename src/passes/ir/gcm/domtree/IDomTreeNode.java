package passes.ir.gcm.domtree;

import passes.ir.IBBRelationship;

import java.util.Set;

public interface IDomTreeNode<T extends IDomTreeNode<T>> extends IBBRelationship<T> {
    Set<T> getEntryBlocks();
    Set<T> getExitBlocks();
    void setDomFather(T father);
    void addDomSon(T son);
}
