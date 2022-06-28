package ir.values;

import ir.User;
import ir.Type;
import ir.values.instructions.BinaryInst;

/**
 * Instruction class is the base class for all the IR instructions.
 * Various kinds of instruction inherit it and are tagged with an
 * InstCategory in cat field for distinction.
 * <br>
 * Type for an Instruction depends on its category.
 * @see <a href="https://github.com/hdoc/llvm-project/blob/release/13.x/llvm/include/llvm/IR/Instruction.h">
 *     LLVM IR Source</a>
 * @see <a href="https://llvm.org/docs/LangRef.html#instruction-reference">
 *     LLVM LangRef: Instruction</a>
 */
public class Instruction extends User {

    /**
     * Each instruction instance has a field "cat" containing a InstCategory instance
     * as a tag distinguishing different instruction types.
     * <br>
     * NOTICE: The ordinal of each enum will be use for distinguishing different types
     * of operations. Be careful to move them or add new ones.
     */
    public enum InstCategory {
        // Operations
        ADD, SUB, MUL, DIV,     // Arithmetic Operations
        LT, GT, EQ, NE, LE, GE, // Relational (Comparison) Operations
        AND, OR,                // Logical Operations
        // Terminators
        RET, BR,
        // Invocation
        CALL,
        // Memory operations
        ZEXT, ALLOCA, LOAD, STORE,
        // Others
        GEP;

        public boolean isArithmeticBinary() {
            return this.ordinal() <= InstCategory.DIV.ordinal();
        }

        public boolean isRelationalBinary() {
            return InstCategory.LT.ordinal() <= this.ordinal()
                    && this.ordinal() <= InstCategory.GE.ordinal();
        }

        public boolean isTerminator() {
            return InstCategory.RET.ordinal() <= this.ordinal()
                    && this.ordinal() <= InstCategory.BR.ordinal();
        }
    }


    //<editor-fold desc="Fields">
    /**
     * An InstCategory instance indicating an instruction type.
     */
    public InstCategory cat;

    /**
     * If an instruction has result, a name (register) should be
     * assigned for the result yielded when naming a Module.
     * Namely, hasResult = true means an instruction needs a name.
     * <br>
     * Most of the instructions have results (by default this field
     * is initialized as true), e.g.
     * <ul>
     *     <li>Binary instructions yield results.</li>
     *     <li>Alloca instruction yield an addresses as results.</li>
     *     <li>ZExt instruction yield extended results.</li>
     * </ul>
     * Terminators and Store instructions have no results, which need
     * to be manually set as false by their constructors.
     */
    public boolean hasResult = true;

    /**
     * Reference of the basic block where the instruction lands.
     */
    private BasicBlock bb;
    //</editor-fold>


    //<editor-fold desc="Constructors">
    public Instruction(Type type, InstCategory tag, BasicBlock bb){
        super(type);
        this.cat = tag;
        this.bb = bb;
    }
    //</editor-fold>


    //<editor-fold desc="Is?">
    public boolean isAdd   () {return this.cat == InstCategory.ADD;}
    public boolean isSub   () {return this.cat == InstCategory.SUB;}
    public boolean isMul   () {return this.cat == InstCategory.MUL;}
    public boolean isDiv   () {return this.cat == InstCategory.DIV;}
    public boolean isAnd   () {return this.cat == InstCategory.AND;}
    public boolean isOr    () {return this.cat == InstCategory.OR;}
    public boolean isRet   () {return this.cat == InstCategory.RET;}
    public boolean isBr    () {return this.cat == InstCategory.BR;}
    public boolean isCall  () {return this.cat == InstCategory.CALL;}
    public boolean isAlloca() {return this.cat == InstCategory.ALLOCA;}
    public boolean isLoad  () {return this.cat == InstCategory.LOAD;}
    public boolean isStore () {return this.cat == InstCategory.STORE;}
    public boolean isIcmp  () {return this.cat.isRelationalBinary();}
    //</editor-fold>


    //<editor-fold desc="Methods">

    /**
     * Get the Basic Block where the instruction lands.
     * @return Reference of the BB.
     */
    public BasicBlock getBB() {
        return this.bb;
    }
    //</editor-fold>

}
