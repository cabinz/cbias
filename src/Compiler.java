/**
 * Compiler is the program to be run in the console.
 * Class Compiler is just a wrapper parsing commandline arguments and launching
 * the underlying compiler driver who controls the concrete compilation flow.
 */
public class Compiler {

    private final static CompileConfig config = new CompileConfig();

    private static final Driver driver = new Driver();

    public static void main(String[] args){
        for (int i = 0; i < args.length; i++){
            if (args[i].endsWith(".sy")){
                // path to the source is given
                config.source = args[i];
            }
            else if ("-o".equals(args[i])){
                // presume output path is given
                config.ASMout = args[++i];
            }
            else if ("-emit-llvm".equals(args[i])) {
                // presume output path is given
                config.llOut = args[++i];
            }
            // todo: -h for help info
            // todo: -O1/2 for opt
            // todo: tackle with unexpected args
        }
        // todo: change to use the 3rd-party arg parser package

        try {
            driver.launch(config);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}