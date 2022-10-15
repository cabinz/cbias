package passes.ir.inline;

import ir.Use;
import ir.Value;
import ir.types.IntegerType;
import ir.types.PointerType;
import ir.types.VoidType;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.Instruction;
import ir.values.instructions.*;
import passes.ir.DummyValue;

import java.util.*;

/**
 * <p>Tool for cloning a function.</p>
 * <p>This class accepts an actual call of a function, and generates codes according to the function called.</p>
 */
public class ClonedFunction {

    private final Function function;
    private final List<Value> aps;

    private final Map<Value, Value> valueMap = new HashMap<>();
    private final Map<BasicBlock, BasicBlock> basicBlockMap = new HashMap<>();
    private final BasicBlock entryBlock, exitBlock;

    private BasicBlock returnCollectBlock;
    private PhiInst returnCollectPhi = null;

    public ClonedFunction(Function function, List<Value> aps, BasicBlock exitBlock){
        this.function = function;
        this.aps = aps;
        this.exitBlock = exitBlock;
        step1();
        step2();
        this.entryBlock = basicBlockMap.get(function.getEntryBB());
    }

    public BasicBlock getEntryBlock() {
        return entryBlock;
    }

    public Value getExitValue(){
        return returnCollectPhi;
    }

    public ArrayList<BasicBlock> getBasicBlocks(){
        var res = new ArrayList<>(basicBlockMap.values());
        res.add(returnCollectBlock);
        return res;
    }

    private Map<Integer,Value> cloneOps(Instruction source){
        Map<Integer,Value> map = new HashMap<>();
        for (Use use : source.getOperands()) {
            var usee = use.getUsee();
            Value value;
            if(usee instanceof BasicBlock){
                value = basicBlockMap.get(usee);
            }else{
                value = valueMap.getOrDefault(usee, usee);
            }
            map.put(use.getOperandPos(), value);
        }
        return map;
    }

    private Instruction cloneInst(Instruction source){
        var type = source.getType();
        var category = source.getTag();
        var ops = cloneOps(source);
        if(source instanceof UnaryOpInst){
            return new UnaryOpInst(type,category,ops.get(0));
        }
        if(source instanceof BinaryOpInst){
            return new BinaryOpInst(type,category,ops.get(0),ops.get(1));
        }
        if(source instanceof CallInst){
            int argNum = ops.size()-1;
            ArrayList<Value> args = new ArrayList<>();
            for(int i=1;i<=argNum;i++){
                args.add(ops.get(i));
            }
            return new CallInst((Function) ops.get(0),args);
        }
        if(source instanceof CastInst){
            if(source instanceof CastInst.ZExt){
                return new CastInst.ZExt(ops.get(0));
            }
            if(source instanceof CastInst.Fptosi){
                return new CastInst.Fptosi(ops.get(0),(IntegerType) type);
            }
            if(source instanceof CastInst.Sitofp){
                return new CastInst.Sitofp(ops.get(0));
            }
            if(source instanceof CastInst.Bitcast){
                return new CastInst.Bitcast(ops.get(0),type);
            }
        }
        if(source instanceof GetElemPtrInst){
            var indexNum = ops.size()-1;
            ArrayList<Value> indices = new ArrayList<>();
            for(int i=1;i<=indexNum;i++){
                indices.add(ops.get(i));
            }
            return new GetElemPtrInst(type, ops.get(0),indices);
        }
        if(source instanceof MemoryInst){
            if(source instanceof MemoryInst.Store){
                return new MemoryInst.Store(ops.get(0),ops.get(1));
            }
            if(source instanceof MemoryInst.Load){
                return new MemoryInst.Load(type,ops.get(0));
            }
            if(source instanceof MemoryInst.Alloca){
                return new MemoryInst.Alloca(((PointerType)type).getPointeeType());
            }
        }
        if(source instanceof PhiInst){
            var srcPhi = (PhiInst) source;
            var phi = new PhiInst(type);
            for (BasicBlock entry : srcPhi.getEntries()) {
                var value = srcPhi.findValue(entry);
                phi.addMapping(basicBlockMap.get(entry), valueMap.getOrDefault(value,value));
            }
            return phi;
        }
        if(source instanceof TerminatorInst){
            if(source instanceof TerminatorInst.Br){
                if(((TerminatorInst.Br)source).isCondJmp()){
                    return new TerminatorInst.Br(ops.get(0), (BasicBlock) ops.get(1), (BasicBlock) ops.get(2));
                }else{
                    return new TerminatorInst.Br((BasicBlock) ops.get(0));
                }
            }
            if(source instanceof TerminatorInst.Ret){
                if(returnCollectPhi!=null){
                    returnCollectPhi.addMapping(basicBlockMap.get(source.getBB()),ops.get(0));
                }
                return new TerminatorInst.Br(returnCollectBlock);
            }
        }
        throw new RuntimeException("Unable to clone instruction of type "+source.getClass());
    }

    /**
     * Step1, construct:
     * - All values with UndefValue.
     * - Empty corresponding blocks.
     * - Return collect block.
     */
    private void step1(){
        // value map, basic block map
        for (BasicBlock basicBlock : function) {
            for (Instruction instruction : basicBlock) {
                valueMap.put(instruction, new DummyValue(VoidType.getType()));
            }
            basicBlockMap.put(basicBlock, new BasicBlock("INLINED_BLOCK"));
        }
        var fps = function.getArgs();
        for(int i=0;i< fps.size();i++){
            valueMap.put(fps.get(i),aps.get(i));
        }
        // return collect
        returnCollectBlock = new BasicBlock("INLINE_COLLECT");
        if(!function.getType().getRetType().isVoidType()){
            returnCollectPhi = new PhiInst(function.getType().getRetType());
            returnCollectBlock.insertAtFront(returnCollectPhi);
        }
        returnCollectBlock.insertAtEnd(new TerminatorInst.Br(exitBlock));
    }

    /**
     * Step2:
     * - Construct instructions.
     * - Move instructions into BasicBlocks.
     */
    private void step2(){
        for (BasicBlock basicBlock : function) {
            var curBB = basicBlockMap.get(basicBlock);
            for (Instruction instruction : basicBlock) {
                var placeholder = valueMap.get(instruction);
                var clonedInst = cloneInst(instruction);
                placeholder.replaceSelfTo(clonedInst);
                valueMap.put(instruction, clonedInst);
                curBB.insertAtEnd(clonedInst);
            }
        }
    }

}
