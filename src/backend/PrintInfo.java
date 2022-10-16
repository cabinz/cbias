package backend;

/**
 * This class is controlling the output of debug info in command line or file.<br/>
 * Usage: In IntelliJ, pressing 'ctrl', then click the '/' twice
 * to uncomment one line and comment the next opposite line <br/><br/>
 * Details about all four config:
 * <ul>
 *     <li><b>printIR</b>: print the corresponding IR as comment behind the assemble codes</li>
 *     <li><b>printCFG</b>: print the predecessors &amp; successors before a block</li>
 *     <li><b>printRegInfo</b>: print the spilled virtual registers before a function</li>
 *     <li><b>printPeepHole</b>: print the assemble code optimized by peepHole in command line</li>
 * </ul>
 */
public class PrintInfo {

//    public static boolean printIR = true;
    public static boolean printIR = false;
//    public static boolean printCFG = true;
    public static boolean printCFG = false;
//    public static boolean printRegInfo = true;
    public static boolean printRegInfo = false;
//    public static boolean printPeepHole = true;
    public static boolean printPeepHole = false;
}
