package frontend;

import ir.Module;
import ir.Value;
import ir.types.FunctionType;
import ir.types.IntegerType;
import ir.Type;
import ir.values.BasicBlock;
import ir.values.Constant;
import ir.values.Function;
import ir.values.Instruction;

import java.math.BigInteger;
import java.util.ArrayList;

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
    }

    //</editor-fold>


    //<editor-fold desc="Methods">

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
        String strRetType = ctx.getChild(0).getText();
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
        FunctionType funcType = new FunctionType(retType, argTypes);
        Function function = builder.buildFunction(funcName, funcType);
        getModule().functions.add(function);
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
                tailInst.cat != Instruction.InstCategory.BR
                        && tailInst.cat != Instruction.InstCategory.RET) {
            if (function.type instanceof FunctionType f) {
                if (f.getRetType().isVoidType()) {
                    builder.buildRet();
                }
                if (f.getRetType().isInteger()) {
                    builder.buildRet(Constant.ConstInt.get(0)); // Return 0 by default.
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
    public Void visitFuncFParam(SysYParser.FuncFParamContext ctx) {
        // todo: Array as function arguments
        // todo: float as function arguments
        // Integer argument
        tmpType = IntegerType.getI32();
        return null;
    }

    /**
     * block : '{' (blockItem)* '}'
     */
    @Override
    public Void visitBlock(SysYParser.BlockContext ctx) {
        scope.scopeIn();
        ctx.blockItem().forEach(this::visit);
        scope.scopeOut();
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
            var ret = builder.buildRet(tmpVal);
        }
        // If not, return void.
        else {
            builder.buildRet();
        }
        return null;
    }

    /**
     * number
     *     : IntConst      # numInt
     *     | FloatConst    # numFloat
     */
//    @Override
//    public Void visitNumber(SysYParser.NumberContext ctx) {
//        tmpVal = Constant.ConstInt.get();
//    }

    /**
     * intConst
     *     : DecIntConst
     *     | OctIntConst
     *     | HexIntConst
     *     ;
     */
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

        tmpVal = Constant.ConstInt.get(val);

        return null;
    }
    //</editor-fold>
}
