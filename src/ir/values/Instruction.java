package ir.values;

import ir.Use;
import ir.User;
import ir.Type;
import ir.Value;

import java.util.List;

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
public abstract class Instruction extends User {

    /**
     * Each instruction instance has a field "cat" containing a InstCategory instance
     * as a tag distinguishing different instruction types.
     * <br>
     * NOTICE: The ordinal of each enum will be use for distinguishing different types
     * of operations. Be careful to move them or add new ones.
     */
    public enum InstCategory {
        /*
         Arithmetic Operations: Integer and floating point.
         */
        ADD, SUB, MUL, DIV,
        FADD, FSUB, FMUL, FDIV,
        FNEG,

        /*
        Relational Operations: Integer and floating point .
         */
        LT, GT, EQ, NE, LE, GE,
        FLT, FGT, FEQ, FNE, FLE, FGE,

        // Logical Operations
        AND, OR,
        // Terminators
        RET, BR,
        // Invocation
        CALL,
        // Memory operations
        ALLOCA, LOAD, STORE,
        // Casting operations
        ZEXT, FPTOSI, SITOFP,
        // Others
        GEP, PHI;


        public boolean isArithmeticBinary() {
            return this.ordinal() <= InstCategory.FNEG.ordinal();
        }

        public boolean isRelationalBinary() {
            return InstCategory.LT.ordinal() <= this.ordinal()
                    && this.ordinal() <= InstCategory.FGE.ordinal();
        }

        public boolean isIntRelationalBinary() {
            return InstCategory.LT.ordinal() <= this.ordinal()
                    && this.ordinal() <= InstCategory.GE.ordinal();
        }

        public boolean isFloatRelationalBinary() {
            return InstCategory.FLT.ordinal() <= this.ordinal()
                    && this.ordinal() <= InstCategory.FGE.ordinal();

        }

        public boolean isTerminator() {
            return InstCategory.RET.ordinal() <= this.ordinal()
                    && this.ordinal() <= InstCategory.BR.ordinal();
        }
    }

    /**
     * An InstCategory instance indicating an instruction type.
     */
    private final InstCategory tag;

    public InstCategory getTag() {
        return tag;
    }

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
    private BasicBlock bb = null;

    public BasicBlock getBB() {
        return this.bb;
    }

    public void setBB(BasicBlock bb) {
        this.bb = bb;
    }

    public Instruction(Type type, InstCategory tag) {
        super(type);
        this.tag = tag;
    }


    public boolean isAdd   () {return this.getTag() == InstCategory.ADD;}
    public boolean isSub   () {return this.getTag() == InstCategory.SUB;}
    public boolean isMul   () {return this.getTag() == InstCategory.MUL;}
    public boolean isDiv   () {return this.getTag() == InstCategory.DIV;}
    public boolean isAnd   () {return this.getTag() == InstCategory.AND;}
    public boolean isOr    () {return this.getTag() == InstCategory.OR;}
    public boolean isRet   () {return this.getTag() == InstCategory.RET;}
    public boolean isBr    () {return this.getTag() == InstCategory.BR;}
    public boolean isCall  () {return this.getTag() == InstCategory.CALL;}
    public boolean isAlloca() {return this.getTag() == InstCategory.ALLOCA;}
    public boolean isLoad  () {return this.getTag() == InstCategory.LOAD;}
    public boolean isStore () {return this.getTag() == InstCategory.STORE;}
    public boolean isIcmp  () {return this.getTag().isIntRelationalBinary();}
    public boolean isGEP   () {return this.getTag() == InstCategory.GEP;}
    public boolean isFcmp  () {return this.getTag().isFloatRelationalBinary();}
    public boolean isZext  () {return this.getTag() == InstCategory.ZEXT;}

    /**
     * Remove the instruction from the BasicBlock holding it.
     * All related Use links will also be removed.
     */
    public void removeSelf() {
        this.getBB().removeInst(this);
    }


    /**
     * Insert a new one at the front of the instruction.
     * @param inst The instruction to be inserted.
     */
    public void insertBefore(Instruction inst) {
        var instList = this.getBB().instructions;
        instList.add(instList.indexOf(this), inst);
        inst.setBB(this.getBB());
    }

    /**
     * Insert a new instruction as the next one of the current.
     * @param inst The instruction to be inserted.
     */
    public void insertAfter(Instruction inst) {
        var instList = this.getBB().instructions;
        instList.add(instList.indexOf(this) + 1, inst);
        inst.setBB(this.getBB());
    }

    public void replaceSelfTo(Value value){
        for (Use use : this.getUses()) {
            use.setUsee(value);
        }
        removeSelf();
    }
}
