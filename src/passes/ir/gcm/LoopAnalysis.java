package passes.ir.gcm;

import java.util.*;

public class LoopAnalysis {

    static class Loop{
        public BasicBlock loopHead;
        public Loop outerLoop;
        private Integer depth;

        public Loop(BasicBlock loopHead){
            this.loopHead = loopHead;
        }

        public int getDepth() {
            if(depth==null){
                if(outerLoop==null){
                    depth = 1;
                }else{
                    depth = outerLoop.getDepth()+1;
                }
            }
            return depth;
        }

    }

    private final GlobalCodeMotionRaw gcm;
    
    private final BasicBlock functionEntry;

    private LoopAnalysis(GlobalCodeMotionRaw gcm){
        this.gcm = gcm;
        BasicBlock functionEntry = null;
        for (BasicBlock possibleEntry : gcm.getBasicBlockMap().values()) {
            if(possibleEntry.getDomFather()==null){
                functionEntry = possibleEntry;
                break;
            }
        }
        if(functionEntry==null){
            throw new RuntimeException("Unable to find function entry.");
        }
        this.functionEntry = functionEntry;
    }

    private void dfsForDomDepth(BasicBlock basicBlock, int currentDepth){
        basicBlock.setDomDepth(currentDepth);
        for (BasicBlock domSon : basicBlock.getDomSons()) {
            dfsForDomDepth(domSon, currentDepth+1);
        }
    }

    private void generateDomDepthInfo() {
        dfsForDomDepth(functionEntry,0);
    }

    private final Map<BasicBlock, Loop> basicBlockLoopMap = new HashMap<>();
    
    private TreeSet<Loop> dfsForLoopInfo(BasicBlock basicBlock, Set<BasicBlock> blocksInPath){
        var activeLoops = new TreeSet<Loop>(Comparator.comparingInt(loop -> loop.loopHead.getDomDepth()));
        blocksInPath.add(basicBlock);
        for (BasicBlock domSon : basicBlock.getDomSons()) {
            var result = dfsForLoopInfo(domSon, blocksInPath);
            activeLoops.addAll(result);
        }
        blocksInPath.remove(basicBlock);

        for (BasicBlock exitBlock : basicBlock.getExitBlocks()) {
            if(blocksInPath.contains(exitBlock)&&!basicBlockLoopMap.containsKey(exitBlock)){
                basicBlockLoopMap.put(exitBlock, new Loop(exitBlock));
                activeLoops.add(basicBlockLoopMap.get(exitBlock));
            }
        }

        basicBlock.setLoop(activeLoops.isEmpty()?null:activeLoops.last());
        if(basicBlockLoopMap.containsKey(basicBlock)){
            var currentLoop = basicBlockLoopMap.get(basicBlock);
            activeLoops.remove(basicBlockLoopMap.get(basicBlock));
            if(activeLoops.isEmpty()){
                currentLoop.outerLoop = null;
            }else{
                currentLoop.outerLoop = activeLoops.last();
            }
        }

        return activeLoops;
    }

    private void generateLoopInfo(){
        dfsForLoopInfo(functionEntry, new HashSet<>());
    }

    private void __analysis__(){
        generateDomDepthInfo();
        generateLoopInfo();
    }

    public static void analysis(GlobalCodeMotionRaw gcm){
        (new LoopAnalysis(gcm)).__analysis__();
    }

}
