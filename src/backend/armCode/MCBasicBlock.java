package backend.armCode;


import java.util.ArrayList;
import java.util.LinkedList;

/**
 * The MCBasicBlock is a sequential  ARM instruction.
 *
 */
public class MCBasicBlock {

    private static int count = 0;

    //<editor-fold desc="Fields">
    private LinkedList<MCInstruction> instructionList;
    private String label;

    private MCFunction belongingFunction;

    private ArrayList<MCBasicBlock> predecessors;
    private MCBasicBlock falseSuccessor;
    private MCBasicBlock trueSuccessor;
    //</editor-fold>


    //<editor-fold desc="Useful methods">
    public void prependInstruction(MCInstruction inst) {instructionList.addFirst(inst);}
    public void appendInstruction(MCInstruction inst) {instructionList.addLast(inst);}

    public void addPredecessor(MCBasicBlock BB) {predecessors.add(BB);}
    //</editor-fold>


    //<editor-fold desc="Getter & Setter">
    public LinkedList<MCInstruction> getInstructionList() {return instructionList;}
    public void setInstructionList(LinkedList<MCInstruction> instructionList) {this.instructionList = instructionList;}

    public String getName() {return label;}
    public void setName(String label) {this.label = label;}

    public MCFunction getBelongingFunction() {return belongingFunction;}
    public void setBelongingFunction(MCFunction belongingFunction) {this.belongingFunction = belongingFunction;}

    public ArrayList<MCBasicBlock> getPredecessors() {return predecessors;}
    public void setPredecessors(ArrayList<MCBasicBlock> predecessors) {this.predecessors = predecessors;}

    public MCBasicBlock getFalseSuccessor() {return falseSuccessor;}
    public void setFalseSuccessor(MCBasicBlock falseSuccessor) {this.falseSuccessor = falseSuccessor;}

    public MCBasicBlock getTrueSuccessor() {return trueSuccessor;}
    public void setTrueSuccessor(MCBasicBlock trueSuccessor) {this.trueSuccessor = trueSuccessor;}
    //</editor-fold>


    //<editor-fold desc="Constructor">
    public MCBasicBlock(MCFunction belongingFunction) {
        this.belongingFunction = belongingFunction;
        instructionList = new LinkedList<MCInstruction>();
        label = ".BLOCK_" + count;
        count++;
    }
    //</editor-fold>

}
