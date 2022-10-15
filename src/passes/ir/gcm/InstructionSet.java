package passes.ir.gcm;

import ir.Use;
import ir.Value;
import ir.values.BasicBlock;
import ir.values.Instruction;
import ir.values.instructions.PhiInst;

import java.util.*;

/**
 * <p>A tool class to judge same codes.</p>
 * <p>
 * This tool can distinguish codes with commutative law and so on.
 * It is achieved through the function getFeatures, which extract features of an instruction.
 * </p>
 */
public class InstructionSet {

    static class Node<E>{
        E value;
        Node<E> next;

        public Node(E value, Node<E> prev){
            this.value = value;
            this.next = prev;
        }

    }

    private final HashMap<Integer, Node<Instruction>> map = new HashMap<>();

    public boolean add(Instruction instruction){
        if(contains(instruction)) return false;
        int hashCode = hash(instruction);
        map.put(hashCode, new Node<>(instruction, map.get(hashCode)));
        return true;
    }

    public boolean contains(Instruction instruction){
        int hashCode = hash(instruction);
        for(Node<Instruction> it=map.get(hashCode);it!=null;it=it.next){
            if(areInstEqual(it.value, instruction)) return true;
        }
        return false;
    }

    public Instruction get(Instruction instruction){
        int hashCode = hash(instruction);
        for(Node<Instruction> it=map.get(hashCode);it!=null;it=it.next){
            if(areInstEqual(it.value, instruction)) return it.value;
        }
        return null;
    }

    static Map<Integer, Value> getOperandMap(Instruction instruction){
        var ret = new HashMap<Integer,Value>();
        for (Use use : instruction.getOperands()) {
            ret.put(use.getOperandPos(), use.getUsee());
        }
        return ret;
    }

    private static boolean areInstEqual(Instruction a, Instruction b){
        return Objects.equals(getFeatures(a),getFeatures(b));
    }

    private static int hash(Instruction instruction){
        ArrayList<Object> features = getFeatures(instruction);
        return Arrays.hashCode(features.toArray());
    }

    private static ArrayList<Object> getFeatures(Instruction instruction) {
        ArrayList<Object> features = new ArrayList<>();
        var instTag = instruction.getTag();
        switch (instTag) {
            case ADD, FADD, MUL, FMUL, AND, OR, EQ, FEQ, NE, FNE -> {
                Set<Value> operandSet = new HashSet<>();
                operandSet.add(instruction.getOperandAt(0));
                operandSet.add(instruction.getOperandAt(1));
                features.add(instruction.getTag());
                features.add(operandSet);
            }
            case SUB, FSUB, DIV, FDIV, GEP, FNEG, ZEXT, FPTOSI, SITOFP, PTRCAST -> {
                features.add(instruction.getTag());
                features.add(getOperandMap(instruction));
            }
            case LT, FLT, LE, FLE, GT, FGT, GE, FGE -> {
                Instruction.InstCategory tag;
                Map<Integer, Value> operandMap = new HashMap<>();
                switch (instTag){
                    case LT, FLT, LE, FLE -> {
                        tag = instruction.getTag();
                        operandMap.put(0, instruction.getOperandAt(0));
                        operandMap.put(1, instruction.getOperandAt(1));
                    }
                    case GT, FGT, GE, FGE -> {
                        tag = switch (instTag){
                            case GT -> Instruction.InstCategory.LT;
                            case FGT -> Instruction.InstCategory.FLT;
                            case GE -> Instruction.InstCategory.LE;
                            case FGE -> Instruction.InstCategory.FLE;
                            default -> throw new RuntimeException("Impossible branch.");
                        };
                        operandMap.put(0, instruction.getOperandAt(1));
                        operandMap.put(1, instruction.getOperandAt(0));
                    }
                    default -> throw new RuntimeException("Impossible branch.");
                }
                features.add(tag);
                features.add(operandMap);
            }
            default -> throw new RuntimeException("Impossible branch. Tag="+instruction.getTag());
//            case PHI -> {
//                var phiInst = (PhiInst) instruction;
//                Map<BasicBlock, Value> phiMapping = new HashMap<>();
//                for (BasicBlock entry : phiInst.getEntries()) {
//                    phiMapping.put(entry, phiInst.findValue(entry));
//                }
//                features.add(instruction.getTag());
//                features.add(phiMapping);
//            }
        }
        return features;
    }

}
