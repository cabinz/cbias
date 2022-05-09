package frontend;

import ir.Module;
import ir.values.Function;
import ir.values.BasicBlock;
import ir.values.instructions.Instruction;

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;

/**
 * An IREmitter object is to output the in-memory IR data structures to a file in plain-text form.
 */
public class IREmitter {
    private String targetFilePath;
    /**
     * Value of the name to be retrieved by getNewName().
     * It's an incremental counter.
     */
    private int nextName = 0;


    /**
     * The IR text will cover the original content if the file already exists.
     * @param targetFilePath Path of the file where output lands.
     */
    public IREmitter(String targetFilePath) {
        this.targetFilePath = targetFilePath;
    }

    private String getNewName() {
        return String.valueOf(nextName++);
    }

    /**
     * To name a module, ie. to assign names for each element in the module.
     * @param m The module object to be named through.
     */
    private void nameModule(Module m) {
        // todo: name global variables in the module
        m.functions.forEach(
                func -> {
                    nextName = 0; // Reset the counter for a new environment.
                    // todo: built-in function branching
                    // Assign names for each function argument.
                    func.getArgs().forEach(funcArg -> {
                        funcArg.name = "%" + getNewName();
                    });
                    // Assign names for each basic block in the function.
                    func.bbs.forEach(bb -> {
                        bb.name = getNewName();
                        // todo: when does an instruction need a name?
                    });
                }
        );
    }


    public void emit(Module m) throws IOException {

        // Initialize the module by assigning names.
        nameModule(m);
        // Initialize a string builder.
        StringBuilder strBuilder = new StringBuilder();

        /*
         Build the whole file as a string in the string builder object.
         */

        // Emit IR text of the module by looping through all the functions in it.
        for (Function func : m.functions) {
            // todo: built-in functions need nothing below.
            // Head of a function: prototype of it.
            strBuilder.append("define dso_local ")
                    .append(func.toString())
                    .append("{\n");
            // Body of a function: basic blocks in it.
            for (BasicBlock bb : func.bbs) {
                // todo: emit the entry of a basic block
                for (Instruction inst : bb.instructions) {
                    strBuilder.append(inst.toString())
                            .append("\n");
                }
            }
            // Tail of a function: A right bracket to close it.
            strBuilder.append("}\n");
        }

        /*
        Write the text in the string builder object into the target text file.
        The existed content in the file will be covered.
         */
        FileWriter fileWriter = new FileWriter(targetFilePath);
        fileWriter.append(strBuilder);
        fileWriter.close();
    }

}
