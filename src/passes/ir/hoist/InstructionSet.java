package passes.ir.hoist;

import ir.Use;
import ir.Value;
import ir.values.Instruction;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    public void add(Instruction instruction){
        if(contains(instruction)) return;
        int hashCode = hash(instruction);
        map.put(hashCode, new Node<>(instruction, map.get(hashCode)));
    }

    public boolean contains(Instruction instruction){
        int hashCode = hash(instruction);
        for(Node<Instruction> it=map.get(hashCode);it!=null;it=it.next){
            if(isEqual(it.value, instruction)) return true;
        }
        return false;
    }

    public Instruction get(Instruction instruction){
        int hashCode = hash(instruction);
        for(Node<Instruction> it=map.get(hashCode);it!=null;it=it.next){
            if(isEqual(it.value, instruction)) return it.value;
        }
        return null;
    }

    static Map<Integer, Value> getOperandMap(Instruction instruction){
        var ret = new HashMap<Integer,Value>();
        for (Use use : instruction.operands) {
            ret.put(use.getOperandPos(), use.getUsee());
        }
        return ret;
    }

    static boolean isEqual(Instruction a, Instruction b){
        return a.cat==b.cat&&Objects.equals(getOperandMap(a),getOperandMap(b));
    }

    static int hash(Instruction instruction){
        return Objects.hash(instruction.cat,getOperandMap(instruction).hashCode());
    }

}
