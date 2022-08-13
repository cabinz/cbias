package passes.ir.analysis;

import ir.values.BasicBlock;
import passes.ir.gcm.GlobalCodeMotionRaw;

import java.util.*;
import java.util.function.Consumer;

public class LoopAnalysis<BasicBlock extends ILoopAnalysis<BasicBlock>> {

    public class Loop {
        public BasicBlock loopHead;
        public Loop outerLoop;
        private Integer depth;

        public Loop(BasicBlock loopHead) {
            this.loopHead = loopHead;
        }

        public int getDepth() {
            if (depth == null) {
                if (outerLoop == null) {
                    depth = 1;
                } else {
                    depth = outerLoop.getDepth() + 1;
                }
            }
            return depth;
        }

    }

    private final BasicBlock functionEntry;

    private LoopAnalysis(Map<ir.values.BasicBlock, BasicBlock> basicBlockMap) {
        BasicBlock functionEntry = null;
        for (BasicBlock possibleEntry : basicBlockMap.values()) {
            if (possibleEntry.getDomFather() == null) {
                functionEntry = possibleEntry;
                break;
            }
        }
        if (functionEntry == null) {
            throw new RuntimeException("Unable to find function entry.");
        }
        this.functionEntry = functionEntry;
    }

    private void dfsForDomDepth(BasicBlock basicBlock, int currentDepth) {
        basicBlock.setDomDepth(currentDepth);
        for (BasicBlock domSon : basicBlock.getDomSons()) {
            dfsForDomDepth(domSon, currentDepth + 1);
        }
    }

    private void generateDomDepthInfo() {
        dfsForDomDepth(functionEntry, 0);
    }

    private final Map<BasicBlock, Loop> basicBlockLoopMap = new HashMap<>();

    private static <BasicBlock extends ILoopAnalysis<BasicBlock>>
    LoopAnalysis<BasicBlock>.Loop getDeeperLoop(LoopAnalysis<BasicBlock>.Loop loop1, LoopAnalysis<BasicBlock>.Loop loop2){
        if(loop1==null) return loop2;
        if(loop2==null) return loop1;
        return loop1.loopHead.getDomDepth()>=loop2.loopHead.getDomDepth()?loop1:loop2;
    }

    private void markLoopInfo(BasicBlock start, Loop loop) {
        Set<BasicBlock> markedBlocks = new HashSet<>();
        Queue<BasicBlock> markQueue = new ArrayDeque<>();
        Consumer<BasicBlock> addBlock = basicBlock -> {
            if (markedBlocks.contains(basicBlock)) return;
            markedBlocks.add(basicBlock);
            markQueue.add(basicBlock);
        };
        addBlock.accept(start);
        while (!markQueue.isEmpty()) {
            var block = markQueue.remove();
            block.setLoop(getDeeperLoop(block.getLoop(),loop));
            if (block != loop.loopHead) {
                if(block.getLoop().loopHead==block){
                    block.getLoop().outerLoop = getDeeperLoop(block.getLoop().outerLoop, loop);
                }
                for (BasicBlock entryBlock : block.getEntryBlocks()) {
                    addBlock.accept(entryBlock);
                }
            }
        }
    }

    private void dfsForLoopInfo(BasicBlock basicBlock, Set<BasicBlock> blocksInPath) {
        blocksInPath.add(basicBlock);
        for (BasicBlock domSon : basicBlock.getDomSons()) {
            dfsForLoopInfo(domSon, blocksInPath);
        }
        blocksInPath.remove(basicBlock);

        for (BasicBlock exitBlock : basicBlock.getExitBlocks()) {
            if (blocksInPath.contains(exitBlock)) {
                if (!basicBlockLoopMap.containsKey(exitBlock)) {
                    var newLoop = new Loop(exitBlock);
                    newLoop.outerLoop = exitBlock.getLoop();
                    basicBlockLoopMap.put(exitBlock, newLoop);
                }
                markLoopInfo(basicBlock, basicBlockLoopMap.get(exitBlock));
            }
        }
    }

    private void generateLoopInfo() {
        dfsForLoopInfo(functionEntry, new HashSet<>());
    }

    private void __analysis__() {
        generateDomDepthInfo();
        generateLoopInfo();
    }

    public static <BasicBlock extends ILoopAnalysis<BasicBlock>>
    void analysis(Map<ir.values.BasicBlock, BasicBlock> basicBlockMap) {
        (new LoopAnalysis<BasicBlock>(basicBlockMap)).__analysis__();
    }

}
