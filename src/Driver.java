import frontend.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * A Driver object is the one actually coping with the compilation flow,
 * including lexical analysis, parsing, building in-memory IR using traversal
 * on the parse tree, optimization with multiple passes, and target code
 * generation.
 */
public class Driver{

    public void launch(CompileConfig config) throws Exception{
        /* Read file */
        CharStream inputFile = CharStreams.fromFileName(config.source);
//            System.out.println(inputFile.toString()); // Test content read in.

        /* Lexical analysis */
        SysYLexer lexer = new SysYLexer(inputFile);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        /* Parsing */
        SysYParser parser = new SysYParser(tokenStream);
        ParseTree ast = parser.compUnit(); // Retrieve the parse tree (It's called AST but actually a CST).

        /* Intermediate code generation */

        /* Emit the IR text to an output file for testing. */

        /* Intermediate code optimization */
        System.out.println("Optimization has not been done.");

        /* Target code generation */
        System.out.println("Generation of target code has not been done.");

        /* Write file */
        System.out.println("Outputting the target file has not been done.");
    }
}