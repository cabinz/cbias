package passes.mc.RegisterAllocation;

import backend.armCode.MCBasicBlock;
import backend.operand.Register;
import backend.operand.VirtualRegister;

import java.util.HashSet;

/**
 * Liveness information of a block
 */
public class LiveInfo {

    /* Lazy to make it better */
    /**
     * The set of incoming live variables
     */
    public HashSet<Register> in = new HashSet<>();
    /**
     * The set of outgoing live variables
     */
    public HashSet<Register> out = new HashSet<>();
    /**
     * The set of variables which is used before def in the block
     */
    public HashSet<Register> use = new HashSet<>();
    /**
     * The set of variables which is def before use in the block
     */
    public HashSet<Register> def = new HashSet<>();

    private final MCBasicBlock basicBlock;

    public MCBasicBlock getBasicBlock() {return basicBlock;}

    public LiveInfo(MCBasicBlock basicBlock){
        this.basicBlock = basicBlock;
    }

}
