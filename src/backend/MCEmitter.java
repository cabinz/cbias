package backend;

import backend.armCode.MCBasicBlock;
import backend.armCode.MCFunction;
import backend.armCode.MCInstruction;
import backend.operand.Label;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * This class is a Singleton Pattern class, emitting the memory
 * structure of ARM assemble to a '.s' file
 * following the standard of GNU tool chain. <br/>
 * Usage: MCEmitter.get().emitTo(target, outputPath)
 */
public class MCEmitter {

    //<editor-fold desc="Singleton Pattern">
    private static final MCEmitter emitter = new MCEmitter();

    private MCEmitter(){}

    public static MCEmitter get() {return emitter;}
    //</editor-fold>


    /**
     * Emit a prepared ARM assemble file from memory to a '.s' file
     * @param target target assemble object
     * @param outputPath '.s' file path
     */
    public void emitTo(ARMAssemble target, String outputPath) throws IOException {
        /* header */
        StringBuilder strBd = new StringBuilder();
        strBd.append("\t.arch " + target.getArchitecture() + '\n');
        strBd.append("\n");
        strBd.append("\t.text\n");
        strBd.append("\n");

        /* handle each function */
        for (MCFunction f : target) {
            if (f.isExternal()) continue;
            strBd.append("\t.global " + f.getName() + '\n');
            strBd.append(f.getName() + ":\n");
            /* handle each BasicBlock */
            for (MCBasicBlock bb : f) {
                strBd.append("." + bb.getName() + ":\n");
                /* handle each instruction */
                for (MCInstruction mcInstruction : bb) {
                    strBd.append('\t' + mcInstruction.emit() + '\n');
                }
            }
            strBd.append("\n\n");
        }

        /* handle each global variable */
        List<Label> globalVars = target.getGlobalVars();
        if (!globalVars.isEmpty()) {
            strBd.append("\t.data\n");
            strBd.append("\t.align 4\n");
            for (Label label : globalVars) {
                strBd.append("\t.global " + label.emit() + '\n');
                strBd.append(label.emit() + ":\n");
                for (Integer tmp : label.getIntial()) {
                    strBd.append("\t.word\t" + tmp + '\n');
                }
            }
        }

        strBd.append("\n\n");
        strBd.append("\t.end");

        /* write to the file */
        FileWriter fw = new FileWriter(outputPath);
        fw.append(strBd);
        fw.close();
    }
}
