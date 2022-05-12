package frontend;

import ir.Module;
import ir.values.Function;
import ir.values.BasicBlock;
import ir.values.Instruction;

import java.io.FileWriter;
import java.io.IOException;

/**
 * An IREmitter object is to output the in-memory IR data structures to a file in plain-text form.
 * @see <a href="https://llvm.org/docs/LangRef.html">
 *     LLVM Language Reference</a>
 */
public class IREmitter {
    private final String targetFilePath;
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
     * Naming a module, ie. to assign a identifier for each element in the module.
     * <br>
     * Unnamed temporaries are numbered sequentially (using a per-function incrementing counter,
     * starting with 0). Note that basic blocks and unnamed function parameters are included in
     * this numbering.
     * @see <a href="https://llvm.org/docs/LangRef.html#identifiers">
     *     LLVM LangRef: Identifiers</a>
     * @param m The module object to be named through.
     */
    private void nameModule(Module m) {
        // todo: name global variables in the module
        m.functions.forEach(
                func -> {
                    nextName = 0; // Reset the counter for a new environment.
                    // todo: built-in function branching
                    // Assign names for each function argument.
                    for (Function.FuncArg arg : func.getArgs()) {
                        arg.name = "%" + getNewName();
                    }
                    // Assign names (Lx) for each basic block in the function,
                    // and each instruction yielding results (%x) in basic blocks.
                    for (BasicBlock bb : func.bbs) {
                        bb.name = "L" + getNewName();
                        for (Instruction inst : bb.instructions) {
                            if (inst.hasResult) {
                                inst.name = "%" + getNewName();
                            }
                        }
                    }
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
