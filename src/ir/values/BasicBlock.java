package ir.values;

import ir.Value;
import ir.Type;
import ir.values.instructions.Instruction;

import java.util.ArrayList;

public class BasicBlock extends Value {

    //<editor-fold desc="Fields">
    public String name;
    private ArrayList<BasicBlock> predecessors;
    private ArrayList<BasicBlock> successors;
    public final ArrayList<Instruction> instructions = new ArrayList<>();
    //</editor-fold>

    //<editor-fold desc="Constructors">
    public BasicBlock(String name) {
        super(Type.LabelType.getType());

        this.name = name;
    }
    //</editor-fold>
}
