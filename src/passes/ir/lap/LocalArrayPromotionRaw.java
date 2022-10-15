package passes.ir.lap;

import ir.Module;
import ir.Type;
import ir.Use;
import ir.Value;
import ir.types.ArrayType;
import ir.types.PointerType;
import ir.values.Constant;
import ir.values.Function;
import ir.values.GlobalVariable;
import ir.values.Instruction;
import ir.values.constants.ConstArray;
import ir.values.constants.ConstInt;
import ir.values.instructions.CallInst;
import ir.values.instructions.CastInst;
import ir.values.instructions.GetElemPtrInst;
import ir.values.instructions.MemoryInst;
import passes.ir.analysis.DomAnalysis;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Distinguish whether an array is a constant array.
 * If so, promote it as a global constant array, so that we do not need to construct this array every time we call
 * the function.
 */
class LocalArrayPromotionRaw {
    private final Map<ir.values.BasicBlock, BasicBlock> basicBlockMap = new HashMap<>();

    private LocalArrayPromotionRaw() {
    }

    private void dfsForDomDfn(BasicBlock basicBlock, AtomicInteger dfnCount) {
        basicBlock.setDomDfn(dfnCount.incrementAndGet());
        for (BasicBlock domSon : basicBlock.getDomSons()) {
            dfsForDomDfn(domSon, dfnCount);
        }
        basicBlock.setDomDfnR(dfnCount.get());
    }

    private void __optimize__(Module module, Function function) {
        function.forEach(basicBlock -> basicBlockMap.put(basicBlock, new BasicBlock(basicBlock)));
        DomAnalysis.analysis(basicBlockMap);
        dfsForDomDfn(basicBlockMap.get(function.getEntryBB()), new AtomicInteger(0));

        Set<MemoryInst.Alloca> promotableSet = new HashSet<>();
        function.getEntryBB().forEach(instruction -> {
            if (instruction instanceof MemoryInst.Alloca) {
                var alloca = (MemoryInst.Alloca) instruction;
                if (alloca.getAllocatedType().isArrayType()) {
                    promotableSet.add(alloca);
                }
            }
        });
        promotableSet.forEach(alloca -> (new Promoter()).__promote__(module, alloca));
    }

    static class PromoteFailedException extends Exception {
        public PromoteFailedException() {
            super();
        }

        public PromoteFailedException(String reason) {
            super(reason);
        }

    }

    class Promoter {
        int earliestLoad = Integer.MAX_VALUE;
        int latestModify = Integer.MIN_VALUE;
        ArrayList<Object> arrayList;
        int arrayLength;
        ArrayType arrayType;

        private final Map<Instruction, Integer> instructionRanking = new HashMap<>();

        private BasicBlock actualEntry = null;

        private Promoter() {
        }

        private void setValue(int offset, Constant value) {
            int atomLen = arrayLength;
            var arrayList = this.arrayList;
            var type = (ArrayType) arrayType;
            while (type.getElemType().isArrayType()) {
                atomLen /= type.getLen();
                type = (ArrayType) type.getElemType();
                var index = offset / atomLen;
                offset %= atomLen;
                // Set target pos to array
                while (arrayList.size() <= index) {
                    arrayList.add(null);
                }
                if (arrayList.get(index) == null) {
                    arrayList.set(index, new ArrayList<>());
                }
                // Update active array list
                arrayList = (ArrayList<Object>) arrayList.get(index);
            }
            if (atomLen != type.getLen()) {
                throw new RuntimeException();
            }
            var index = offset;
            while (arrayList.size() <= index) {
                arrayList.add(null);
            }
            arrayList.set(index, value);
        }

        private boolean willChangeArray(Function function) {
            if (!function.isExternal()) return true;
            if (function.getName().equals("putint") ||
                    function.getName().equals("putfloat") ||
                    function.getName().equals("putch") ||
                    function.getName().equals("putarray") ||
                    function.getName().equals("putfarray") ||
                    function.getName().equals("memset")) return false;
            return true;
        }

        private void getArrayList(Instruction instruction, Type type, int offset, int atomLen) throws PromoteFailedException {
            // Check dfn first.
            if (!(instruction instanceof MemoryInst.Alloca)) {
                var dfn = basicBlockMap.get(instruction.getBB()).getDomDfn();
                if (dfn < actualEntry.getDomDfnL() || dfn > actualEntry.getDomDfnR()) {
                    throw new PromoteFailedException("Load before store.");
                }
            }
            // Recurse
            if (instruction instanceof GetElemPtrInst) {
                var operands = instruction.getOperands();
                for (int i = 1; i < operands.size(); i++) {
                    var value = instruction.getOperandAt(i);
                    if (!(value instanceof ConstInt)) {
                        atomLen = 0; // Mark cannot determine
                        continue;
                    }
                    var index = ((ConstInt) value).getVal();
                    if (i != 1) {
                        var arrType = (ArrayType) type;
                        atomLen /= arrType.getLen();
                        type = arrType.getElemType();
                    }
                    offset += atomLen * index;
                }
            }
            if (instruction instanceof MemoryInst.Load) {
                if (instructionRanking.containsKey(instruction)) {
                    earliestLoad = Integer.min(earliestLoad, instructionRanking.get(instruction));
                }
                return;
            }
            if (instruction instanceof MemoryInst.Store) {
                if (instructionRanking.containsKey(instruction)) {
                    latestModify = Integer.max(latestModify, instructionRanking.get(instruction));
                } else {
                    throw new RuntimeException("Store inst not in entry bb."); // Use RuntimeException since it cannot happen.
                }
                if (atomLen == 0) {
                    throw new PromoteFailedException("Cannot determine the address stored.");
                }
                var value = instruction.getOperandAt(0);
                if (!(value instanceof Constant)) {
                    throw new PromoteFailedException("Storing dynamic value.");
                }
                setValue(offset, (Constant) value);
                return;
            }
            if (instruction instanceof CallInst) {
                var callee = (Function) instruction.getOperandAt(0);
                if (!willChangeArray(callee)) {
                    return;
                } else {
                    throw new PromoteFailedException("Array is used as an argument of a call.");
                }
            }
            if (instruction instanceof MemoryInst.Alloca || instruction instanceof GetElemPtrInst || instruction instanceof CastInst.Bitcast) {
                for (Use use : instruction.getUses()) {
                    getArrayList((Instruction) use.getUser(), type, offset, atomLen);
                }
                return;
            }
            throw new RuntimeException("Unexpected instruction type " + instruction.getTag());
        }

        private ConstArray buildConstArray(ArrayType arrayType, ArrayList<Object> arrayList) {
            if (arrayList == null) {
                return (ConstArray) arrayType.getZero();
            }
            var list = new ArrayList<Constant>();
            if (!arrayType.getElemType().isArrayType()) {
                for (Object o : arrayList) {
                    if (o == null) {
                        o = arrayType.getElemType().getZero();
                    }
                    list.add((Constant) o);
                }
            } else {
                for (Object o : arrayList) {
                    list.add(buildConstArray((ArrayType) arrayType.getElemType(), (ArrayList<Object>) o));
                }
            }
            return ConstArray.get(arrayType, list);
        }

        /**
         * Remove store AND memset.
         */
        private void removeStoreInstructions(Instruction instruction) {
            if (instruction instanceof CallInst) {
                var callee = (Function) instruction.getOperandAt(0);
                if (Objects.equals(callee.getName(), "memset")) {
                    instruction.markWasted();
                }
                return;
            }
            if (instruction instanceof MemoryInst.Load) {
                return;
            }
            if (instruction instanceof MemoryInst.Store) {
                instruction.markWasted();
                return;
            }
            if (instruction instanceof GetElemPtrInst || instruction instanceof MemoryInst.Alloca || instruction instanceof CastInst.Bitcast) {
                for (Use use : instruction.getUses()) {
                    var user = (Instruction) use.getUser();
                    removeStoreInstructions(user);
                }
                return;
            }
            throw new RuntimeException("Unexpected instruction type " + instruction.getTag());
        }

        private void findAllStore(Instruction instruction) throws PromoteFailedException {
            if (instruction instanceof CallInst || instruction instanceof MemoryInst.Load) {
                return;
            }
            if (instruction instanceof MemoryInst.Store) {
                if (actualEntry == null) {
                    actualEntry = basicBlockMap.get(instruction.getBB());
                } else {
                    if (actualEntry != basicBlockMap.get(instruction.getBB())) {
                        throw new PromoteFailedException("Store in multiple bb.");
                    }
                }
                return;
            }
            if (instruction instanceof GetElemPtrInst || instruction instanceof MemoryInst.Alloca || instruction instanceof CastInst.Bitcast) {
                for (Use use : instruction.getUses()) {
                    var user = (Instruction) use.getUser();
                    findAllStore(user);
                }
                return;
            }
            throw new RuntimeException("Unexpected instruction type " + instruction.getTag());
        }

        private void generateInstructionRanking(MemoryInst.Alloca alloca) throws PromoteFailedException {
            findAllStore(alloca);
            AtomicInteger rankCount = new AtomicInteger(0);
            if (actualEntry == null) {
                actualEntry = basicBlockMap.get(alloca.getBB().getFunc().getEntryBB());
            }
            actualEntry.getRawBasicBlock().forEach(instruction -> instructionRanking.put(instruction, rankCount.incrementAndGet()));
        }

        private void __promote__(Module module, MemoryInst.Alloca alloca) {
            try {
                generateInstructionRanking(alloca);
                arrayList = new ArrayList<>(1);
                arrayType = (ArrayType) alloca.getAllocatedType();
                arrayLength = arrayType.getAtomLen();
                getArrayList(alloca, arrayType, 0, ((ArrayType) alloca.getAllocatedType()).getAtomLen());
                if (latestModify > earliestLoad) {
                    throw new PromoteFailedException("Store after load.");
                }
                var constArray = buildConstArray((ArrayType) alloca.getAllocatedType(), arrayList);
                var globalVariable = new GlobalVariable(null, constArray);
                globalVariable.setConstant();
                module.addGlobalVariable(globalVariable);
                removeStoreInstructions(alloca);
                alloca.replaceSelfTo(globalVariable);
            } catch (PromoteFailedException ignored) {
            }
        }

    }

    static void optimize(Module module, Function function) {
        (new LocalArrayPromotionRaw()).__optimize__(module, function);
    }

}
