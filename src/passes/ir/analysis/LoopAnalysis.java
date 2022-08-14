package passes.ir.analysis;

import java.util.*;
import java.util.function.Consumer;

public class LoopAnalysis<BasicBlock extends ILoopAnalysis<BasicBlock>> {

    public class Loop {
        private final BasicBlock loopHead;
        private Loop outerLoop;

        private final Set<Loop> innerLoops = new HashSet<>();
        private Integer depth;

        /**
         * All basic blocks directly belonging to the loop, excluding the inner loop
         */
        private List<BasicBlock> bbs = new LinkedList<>();

        /**
         * The loop node with backedge to the header
         */
        private List<BasicBlock> latch = new LinkedList<>();

        /**
         * The blocks branch out the loop. In the SysY, all loop are 'while',
         * so that in this compiler all exiting blocks are the 'cond block' or 'break block'.
         * The last block of the loop body never be the exiting block, must be the latch.
         */
        private List<BasicBlock> exiting = new LinkedList<>();

        Loop(BasicBlock loopHead) {
            this.loopHead = loopHead;
        }

        void setOuterLoop(Loop outerLoop) {
            if (this.outerLoop != null) {
                this.outerLoop.innerLoops.remove(this);
            }
            this.outerLoop = outerLoop;
            if(this.outerLoop!=null){
                this.outerLoop.innerLoops.add(this);
            }
        }

        public boolean contains(BasicBlock bb) {
            if (bbs.contains(bb))
                return true;
            else {
                for (var inner : innerLoops)
                    if (inner.contains(bb))
                        return true;
                return false;
            }
        }

        public int getDepth() {
            if (depth == null) {
                if (getOuterLoop() == null) {
                    depth = 1;
                } else {
                    depth = getOuterLoop().getDepth() + 1;
                }
            }
            return depth;
        }

        public BasicBlock getLoopHead() {
            return loopHead;
        }

        public Loop getOuterLoop() {
            return outerLoop;
        }

        public Set<Loop> getInnerLoops() {
            return innerLoops;
        }

        public List<BasicBlock> getBBs() {return bbs;}
        public List<BasicBlock> getLatch() {return latch;}
        public List<BasicBlock> getExiting() {return exiting;}

        public void addBBs(BasicBlock bb) {bbs.add(bb);}
        public void addLatch(BasicBlock bb) {latch.add(bb);}
        public void addExiting(BasicBlock bb) {exiting.add(bb);}
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
    LoopAnalysis<BasicBlock>.Loop getDeeperLoop(LoopAnalysis<BasicBlock>.Loop loop1, LoopAnalysis<BasicBlock>.Loop loop2) {
        if (loop1 == null) return loop2;
        if (loop2 == null) return loop1;
        return loop1.getLoopHead().getDomDepth() >= loop2.getLoopHead().getDomDepth() ? loop1 : loop2;
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
            block.setLoop(getDeeperLoop(block.getLoop(), loop));
            if (block != loop.getLoopHead()) {
                if (block.getLoop().getLoopHead() == block) {
                    block.getLoop().setOuterLoop(getDeeperLoop(block.getLoop().getOuterLoop(), loop));
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
                    newLoop.setOuterLoop(exitBlock.getLoop());
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
        DomAnalysis.analysis(basicBlockMap);
        (new LoopAnalysis<BasicBlock>(basicBlockMap)).__analysis__();
    }

}
