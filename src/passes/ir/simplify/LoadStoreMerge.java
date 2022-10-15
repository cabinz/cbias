package passes.ir.simplify;

import ir.Module;
import ir.Value;
import ir.values.BasicBlock;
import ir.values.GlobalVariable;
import ir.values.Instruction;
import ir.values.instructions.CallInst;
import ir.values.instructions.MemoryInst;
import ir.values.instructions.TerminatorInst;
import passes.ir.IRPass;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Temporary regard global variables as local ones to reduce load/store instructions.
 */
public class LoadStoreMerge implements IRPass {
    @Override
    public void runOnModule(Module module) {
        module.functions.forEach(function -> function.forEach(LoadStoreMerge::optimize));
    }

    private static void optimize(BasicBlock basicBlock){
        Map<Value, Value> lastValueMap = new HashMap<>();
        Set<Value> updatedAddress = new HashSet<>();
        Value lastBufferedAddress = null;
        for (Instruction instruction : basicBlock) {
            if(instruction instanceof MemoryInst.Load){
                var address = instruction.getOperandAt(0);
                if(lastValueMap.containsKey(address)){
                    instruction.replaceSelfTo(lastValueMap.get(address));
                }else{
                    lastValueMap.put(address, instruction);
                }
            }
            if(instruction instanceof MemoryInst.Store){
                var value = instruction.getOperandAt(0);
                var address = instruction.getOperandAt(1);
                if(!(address instanceof GlobalVariable)){
                    if(lastBufferedAddress!=null){
                        lastValueMap.remove(lastBufferedAddress);
                    }
                    lastValueMap.put(address,value);
                    lastBufferedAddress = address;
                }else{
                    lastValueMap.put(address,value);
                    updatedAddress.add(address);
                    instruction.markWasted();
                }
            }
            if(instruction instanceof CallInst || instruction instanceof TerminatorInst){
                flushAllStores(lastValueMap, updatedAddress, instruction);
            }
        }
    }

    /**
     * Store all changed value.
     */
    private static void flushAllStores(Map<Value, Value> lastValueMap, Set<Value> updatedAddress, Instruction instruction) {
        lastValueMap.forEach((address, value)->{
            if(updatedAddress.contains(address)){
                var storeInst = new MemoryInst.Store(value,address);
                storeInst.insertBefore(instruction);
            }
        });
        lastValueMap.clear();
        updatedAddress.clear();
    }

}
