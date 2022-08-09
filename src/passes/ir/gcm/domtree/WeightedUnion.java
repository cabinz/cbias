package passes.ir.gcm.domtree;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class WeightedUnion<BasicBlock extends passes.ir.BasicBlock & IDomTreeNode<BasicBlock>> {
    class NodeAttachment {
        public DomTree<BasicBlock>.NodeAttachment domAttachment;
        public BasicBlock unionFather, value;

        public NodeAttachment(BasicBlock basicBlock, DomTree<BasicBlock>.NodeAttachment domAttachment) {
            this.domAttachment = domAttachment;
            this.unionFather = basicBlock;
            this.value = null;
        }

    }

    private final Map<BasicBlock, NodeAttachment> attachmentMap = new HashMap<>();

    public NodeAttachment getAttachment(BasicBlock basicBlock){
        return attachmentMap.get(basicBlock);
    }

    public WeightedUnion(Map<BasicBlock, DomTree<BasicBlock>.NodeAttachment> domAttachmentMap) {
        domAttachmentMap.forEach((basicBlock, nodeAttachment) -> {
            attachmentMap.put(basicBlock, new NodeAttachment(basicBlock, nodeAttachment));
        });
    }

    public BasicBlock getRoot(BasicBlock basicBlock) {
        var nodeAttachment = attachmentMap.get(basicBlock);
        if (nodeAttachment.unionFather != basicBlock) {
            BasicBlock temp = nodeAttachment.unionFather;
            nodeAttachment.unionFather = getRoot(nodeAttachment.unionFather);
            if (attachmentMap.get(attachmentMap.get(attachmentMap.get(temp      ).value).domAttachment.sdom).domAttachment.dfn <
                attachmentMap.get(attachmentMap.get(attachmentMap.get(basicBlock).value).domAttachment.sdom).domAttachment.dfn) {
                nodeAttachment.value = attachmentMap.get(temp).value;
            }
        }
        return nodeAttachment.unionFather;
    }

    public void union(BasicBlock u, BasicBlock v){
        getRoot(v);
        attachmentMap.get(v).unionFather = u;
    }

}
