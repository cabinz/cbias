package frontend;

import ir.Module;
import ir.Value;
import ir.types.FunctionType;
import ir.types.IntegerType;
import ir.Type;
import ir.types.PointerType;
import ir.values.BasicBlock;
import ir.values.Constant;
import ir.values.Function;
import ir.values.Instruction;
import ir.values.Instruction.InstCategory;
import ir.values.instructions.BinaryInst;
import ir.values.instructions.MemoryInst;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Visitor extends SysYBaseVisitor<Void> {
    //<editor-fold desc="Fields">

    public IRBuilder builder;
    public final Scope scope = new Scope();


    /**
     * Temporary variables for messages passed through layers visited.
     */
    Value tmpVal;
    ArrayList<Type> tmpTypeList;
    Type tmpType;

    //</editor-fold>


    //<editor-fold desc="Constructors">

    public Visitor (IRBuilder builder) {
        this.builder = builder;
        this.initRuntimeFunctions();
    }

    //</editor-fold>


    //<editor-fold desc="Methods">

    /**
     * Add declarations of the runtime library functions to symbol table
     * whose definitions will be linked in after assembling.
     * This method is called by constructor and can be called only once.
     */
    private void initRuntimeFunctions() {
        ArrayList<Type> emptyArgTypeList = new ArrayList<>();
        ArrayList<Type> intArgTypeList = new ArrayList<>(Collections.singletonList(IntegerType.getI32()));
        // getint()
        scope.addDecl("getint",
                builder.buildFunction("getint", FunctionType.getType(
                        IntegerType.getI32(), emptyArgTypeList
                ), true)
        );
        // putint(i32)
        scope.addDecl("putint",
                builder.buildFunction("putint", FunctionType.getType(
                        Type.VoidType.getType(), intArgTypeList
                ), true)
        );// getch()
        scope.addDecl("getch",
                builder.buildFunction("getch", FunctionType.getType(
                        IntegerType.getI32(), emptyArgTypeList
                ), true)
        );
        // putch(i32)
        scope.addDecl("putch",
                builder.buildFunction("putch", FunctionType.getType(
                        Type.VoidType.getType(), intArgTypeList
                ), true)
        );
    }

    /**
     * Get the current module from the builder inside.
     * @return The current module.
     */
    public Module getModule() {
        return builder.getCurModule();
    }

    /*
    Visit methods overwritten.
     */

    @Override
    public Void visitCompUnit(SysYParser.CompUnitContext ctx) {
        super.visitCompUnit(ctx);
        return null;
    }

    /**
     * funcDef : funcType Identifier '(' (funcFParams)? ')' block
     */
    @Override
    public Void visitFuncDef(SysYParser.FuncDefContext ctx) {
//        System.out.println("In FuncDef");

        /*
        Collect object info.
         */

        // Get the function name.
        String funcName = ctx.Identifier().getText();

        // Get the return type. (funcType identifier)
        Type retType;
        String strRetType = ctx.funcType().getText();
        if (strRetType.equals("int")) {
            retType = IntegerType.getI32();
        }
        else if (strRetType.equals("float")) {
            retType = Type.VoidType.getType(); // todo float
        }
        else {
            retType = Type.VoidType.getType();
        }

        // Get the argument list. (Visiting child)
        ArrayList<Type> argTypes = new ArrayList<>();
        if (ctx.funcFParams() != null) {
            visit(ctx.funcFParams());
            argTypes.addAll(tmpTypeList);
        }

        /*
        Build IR.
         */

        // Insert a function into the module and symbol table.
        FunctionType funcType = FunctionType.getType(retType, argTypes);
        Function function = builder.buildFunction(funcName, funcType, false);
        scope.addDecl(funcName, function);

        // Insert a basic block. Then scope in.
        BasicBlock bb = builder.buildBB(funcName + "_ENTRY");
        scope.scopeIn();

        /*
        Process function body. (Visiting child)
         */
        visit(ctx.block());

        /*
        Check the last basic block of the function to see if there is a
        return statement given in the source.
        If not, insert a terminator to the end of it.
         */
        ArrayList<Instruction> instList = builder.getCurBB().instructions;
        Instruction tailInst = (instList.size() == 0) ? null : instList.get(instList.size() - 1);
        // If no instruction in the bb, or the last instruction is not a terminator.
        if (tailInst == null ||
                tailInst.cat != InstCategory.BR
                        && tailInst.cat != InstCategory.RET) {
            if (function.type instanceof FunctionType f) {
                if (f.getRetType().isVoidType()) {
                    builder.buildRet();
                }
                if (f.getRetType().isInteger()) {
                    builder.buildRet(builder.buildConstant(0)); // Return 0 by default.
                }
                // todo: return float
            }
        }

        return null;
    }

    /**
     * funcFParams : funcFParam (',' funcFParam)*
     */
    @Override
    public Void visitFuncFParams(SysYParser.FuncFParamsContext ctx) {
        tmpTypeList = new ArrayList<>();
        ctx.funcFParam().forEach(arg -> {
            visit(arg);
            tmpTypeList.add(tmpType);
        });
        return null;
    }


    /**
     * funcFParam : bType Identifier ('[' ']' ('[' expr ']')* )?
     */
    @Override
    public Void visitScalarFuncFParam(SysYParser.ScalarFuncFParamContext ctx) {
        // todo: float as function arguments
        // Integer argument
        tmpType = IntegerType.getI32();
        return null;
    }

    /**
     * block : '{' (blockItem)* '}'
     * blockItem : decl | stmt
     */
    @Override
    public Void visitBlock(SysYParser.BlockContext ctx) {
        scope.scopeIn(); // Add a new layer of scope (a new symbol table).
        ctx.blockItem().forEach(this::visit);
        scope.scopeOut(); // Pop it out before exiting the scope.
        return null;
    }

    /**
     * stmt : 'return' (expr)? ';'
     */
    @Override
    public Void visitRetStmt(SysYParser.RetStmtContext ctx) {
        // If there is an expression component to be returned,
        // visit child to retrieve it.
        if (ctx.expr() != null) {
            visit(ctx.expr());
            builder.buildRet(tmpVal);
        }
        // If not, return void.
        else {
            builder.buildRet();
        }
        return null;
    }

    /**
     * intConst
     *     : DecIntConst
     *     | OctIntConst
     *     | HexIntConst
     *     ;
     */
    @Override
    public Void visitIntConst(SysYParser.IntConstContext ctx) {
        // If the final result is 0xffff (65535), there might be error
        // causing all conditional assignments below to be missed.
        int val = 0xffff;

        // Integer in decimal format, parse directly.
        if (ctx.DecIntConst() != null) {
            val = Integer.parseInt(ctx.DecIntConst().getText(), 10);
        }
        // Integer in octal format, parse directly in radix of 8.
        else if (ctx.OctIntConst() != null) {
            val = Integer.parseInt(ctx.OctIntConst().getText(), 8);
        }
        // Integer in hexadecimal format, drop the first two characters '0x'
        else if (ctx.HexIntConst() != null) {
            val = Integer.parseInt(ctx.HexIntConst().getText().substring(2), 16);
        }

        tmpVal = builder.buildConstant(val);

        return null;
    }

    /**
     * unaryExp : unaryOp unaryExp # oprUnaryExp
     */
    @Override
    public Void visitOprUnaryExp(SysYParser.OprUnaryExpContext ctx) {
        // Retrieve the expression by visiting child.
        visit(ctx.unaryExp());
        // Integer.
        if (tmpVal.type.isInteger()) {
            // Conduct zero extension on i1.
            if (tmpVal.type.isI1()) {
                builder.buildZExt(tmpVal);
            }
            // Unary operators.
            String op = ctx.unaryExp().getText();
            switch (op) {
                case "-":
                    tmpVal = builder.buildBinary(InstCategory.SUB, builder.buildConstant(0), tmpVal);
                    break;
                case "+":
                    // Do nothing.
                    break;
                case "!":
                    tmpVal = builder.buildBinary(InstCategory.EQ, builder.buildConstant(0), tmpVal);
                    break;
                default:
            }
        }
        // Float.
        else {
            // todo: if it's a float.
        }
        return null;
    }

    /**
     * addExp : mulExp (('+' | '-') mulExp)*
     */
    @Override
    public Void visitAddExp(SysYParser.AddExpContext ctx) {
        // todo: float case
        // Retrieve the 1st mulExp (as the left operand) by visiting child.
        visit(ctx.mulExp(0));
        Value lOp = tmpVal;
        // The 2nd and possibly more MulExp.
        for (int i = 1; i < ctx.mulExp().size(); i++) {
            // Retrieve the next mulExp (as the right operand) by visiting child.
            visit(ctx.mulExp(i));
            Value rOp = tmpVal;
            // Check integer types of two operands.
            if (lOp.type.isI1()) {
                lOp = builder.buildZExt(lOp);
            }
            if (rOp.type.isI1()) {
                rOp = builder.buildZExt(lOp);
            }
            // Generate an instruction to compute result of left and right operands
            // as the new left operand for the next round.
            switch (ctx.getChild(2 * i - 1).getText()) {
                case "+" -> lOp = builder.buildBinary(InstCategory.ADD, lOp, rOp);
                case "-" -> lOp = builder.buildBinary(InstCategory.SUB, lOp, rOp);
                default -> {}
            }
        }

        tmpVal = lOp;

        return null;
    }


    /**
     * mulExp : unaryExp (('*' | '/' | '%') unaryExp)*
     */
    @Override
    public Void visitMulExp(SysYParser.MulExpContext ctx) {
        // todo: float case
        // Retrieve the 1st unaryExp (as the left operand) by visiting child.
        visit(ctx.unaryExp(0));
        Value lOp = tmpVal;
        // The 2nd and possibly more MulExp.
        for (int i = 1; i < ctx.unaryExp().size(); i++) {
            // Retrieve the next unaryExp (as the right operand) by visiting child.
            visit(ctx.unaryExp(i));
            Value rOp = tmpVal;
            // Generate an instruction to compute result of left and right operands
            // as the new left operand for the next round.
            switch (ctx.getChild(2 * i - 1).getText()) {
                case "/" -> lOp = builder.buildBinary(InstCategory.DIV, lOp, rOp);
                case "*" -> lOp = builder.buildBinary(InstCategory.MUL, lOp, rOp);
                case "%" -> { // l % r => l - (l/r)*r
                    BinaryInst div = builder.buildBinary(InstCategory.DIV, lOp, rOp); // l/r
                    BinaryInst mul = builder.buildBinary(InstCategory.MUL, div, rOp); // (l/r)*r
                    lOp = builder.buildBinary(InstCategory.SUB, lOp, mul);
                }
                default -> { }
            }
        }

        tmpVal = lOp;
        return null;
    }

    /**
     * varDef : Identifier ('=' initVal)? # scalarVarDef
     */
    @Override
    public Void visitScalarVarDef(SysYParser.ScalarVarDefContext ctx) {
        // Retrieve the name of the variable defined and check for duplication.
        String varName = ctx.Identifier().getText();
        if (scope.duplicateDecl(varName)) {
            throw new RuntimeException("Duplicate definition of variable name: " + varName);
        }
        // todo: Global variable.
        // todo: float (branching by type)
        MemoryInst.Alloca addrAllocated = builder.buildAlloca(IntegerType.getI32());
        scope.addDecl(varName, addrAllocated);
        // If it's a definition with initialization.
        if (ctx.initVal() != null) {
            visit(ctx.initVal());
            builder.buildStore(tmpVal, addrAllocated);
        }
        return null;
    }

    /**
     * lVal : Identifier ('[' expr ']')*
     * ------------------------------------------
     * stmt : lVal '=' expr ';'     # assignment
     * primaryExp : lVal            # primExpr2
     * ------------------------------------------
     * Notice that besides being a left value for
     * assignment lVal can be a primary expression.
     */
    @Override
    public Void visitScalarLVal(SysYParser.ScalarLValContext ctx) {
        /*
        Retrieve the value defined previously from the symbol table.
         */
        String name  = ctx.Identifier().getText();
        Value val = scope.getValByName(name);

        /*
        If the value does not exist, report the semantic error.
         */
        if (val == null) {
            throw new RuntimeException("Undefined value: " + name);
        }

        /*
        There are two cases for lVal as a grammar symbol:
        1.  If a lVal  can be reduce to a primaryExp,
            in this case is a scalar value (IntegerType or FloatType)
            thus the value can be returned directly.
        2.  Otherwise, a lVal represents a left value,
            which generates an address (PointerType Value)
            designating a memory block for assignment.
         */
        // Case 1, return directly.
        // todo: float type can be returned directly too
        if (val.type.isInteger()) {
            tmpVal = val;
            return null;
        }
        // Case 2, return a PointerType Value.
        if (val.type.isPointerType()) {
            Type pointeeType = ((PointerType) val.type).getPointeeType();
            // ptr* (pointer to pointer)
            if (pointeeType.isPointerType()) {

            }
            // i32*: Return directly.
            else if (pointeeType.isInteger()) {
                tmpVal = val;
                return null;
            }
            // todo: array*, float*
            return null;
        }
        return null;
    }


    /**
     * primaryExp
     *     : '(' expr ')'  # primExpr1
     *     | lVal          # primExpr2
     *     | number        # primExpr3
     * ---------------------------------------------------
     * unaryExp
     *     : primaryExp                         # unary1
     *     | Identifier '(' (funcRParams)? ')'  # unary2
     *     | unaryOp unaryExp                   # unary3
     * ---------------------------------------------------
     * Primary expression represents an independent value
     * that can involve in future operations / computation
     * in the source program.
     * <br>
     * Since primaryExp is a grammar symbol representing
     * pure value, when an lVal (whose IR construct may
     * be in PointerType) is reduced to primExp, it should
     * be checked, and possibly a Load instruction is need
     * to read the memory block onto register to be a pure
     * value for instant use.
     */
    @Override
    public Void visitPrimExpr2(SysYParser.PrimExpr2Context ctx) {
        // todo: branch out if during building a Call
        visit(ctx.lVal());
        // Load the memory block if a PointerType Value is retrieved from lVal.
        if (tmpVal.type.isPointerType()) {
            Type pointeeType = ((PointerType) tmpVal.type).getPointeeType();
            tmpVal = builder.buildLoad(pointeeType, tmpVal);
        }
        return null;
    }

    /**
     * constDef : Identifier '=' constInitVal # scalarConstDef
     * -------------------------------------------------------------------------
     * constDecl
     *     : 'const' bType constDef (',' constDef)* ';'
     * constInitVal
     *     : constExp                                      # scalarConstInitVal
     *     | '{' (constInitVal (',' constInitVal)* )? '}'  # arrConstInitVal
     */
    @Override
    public Void visitScalarConstDef(SysYParser.ScalarConstDefContext ctx) {
        // Retrieve the name of the variable defined and check for duplication.
        String varName = ctx.Identifier().getText();
        if (scope.duplicateDecl(varName)) {
            throw new RuntimeException("Duplicate definition of constant name: " + varName);
        }

        // Retrieve the initialized value by visiting child.
        // Then update the symbol table.
        visit(ctx.constInitVal());
        scope.addDecl(varName, tmpVal);

        return null;
    }


    /**
     * stmt : lVal '=' expr ';' # assignStmt
     */
    @Override
    public Void visitAssignStmt(SysYParser.AssignStmtContext ctx) {
        // Retrieve left value (the address to store) by visiting child.
        visit(ctx.lVal());
        Value addr = tmpVal;
        // Retrieve the value to be stored by visiting child.
        visit(ctx.expr());
        Value val = tmpVal;
        // Build the Store instruction.
        builder.buildStore(val, addr);
        return null;
    }

    /**
     * unaryExp : Identifier '(' (funcRParams)? ')'  # fcallUnaryExp
     * -------------------------------------------------------------
     * funcRParams : funcRParam (',' funcRParam)*
     */
    @Override
    public Void visitFcallUnaryExp(SysYParser.FcallUnaryExpContext ctx) {
        // The identifier needs to be previously defined as a function
        // and in the symbol table.
        String name = ctx.Identifier().getText();
        Value func = scope.getValByName(name);
        if (func == null) {
            throw new RuntimeException("Undefined name: " + name + ".");
        }
        if (!(func.type instanceof FunctionType)) {
            // todo: isType() should belong to Value other than Type
            throw new RuntimeException(name + " is not a function and cannot be invoked.");
        }

        // If the function has argument(s) passed, retrieve them by visiting child(ren).
        ArrayList<Value> args = new ArrayList<>();
        if (ctx.funcRParams() != null) {
            var argCtxs = ctx.funcRParams().funcRParam();
            ArrayList<Type> argTypes = ((FunctionType)func.type).getArgTypes();
            // Loop through both the lists of context and type simultaneously.
            for (int i = 0; i < argCtxs.size(); i++) {
                var argCtx = argCtxs.get(i);
                Type typeArg = argTypes.get(i);
                // Visit child RParam.
                visit(argCtx);
                // Add the argument Value retrieved by visiting to the container.
                args.add(tmpVal);
            }
        }

        // Build a Call instruction.
        tmpVal = builder.buildCall((Function)func, args);

        return null;
    }

    /**
     * funcRParam
     *     : expr    # exprRParam
     *     | STRING  # strRParam
     */
    @Override
    public Void visitStrRParam(SysYParser.StrRParamContext ctx) {
        // todo: Cope with string function argument.
        tmpVal = null;

        return null;
    }


    //</editor-fold>
}
