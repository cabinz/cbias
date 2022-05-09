package frontend;

import ir.Module;
import ir.types.FunctionType;
import ir.types.IntegerType;
import ir.Type;
import ir.values.BasicBlock;
import ir.values.Function;
import ir.values.instructions.Instruction;

import java.util.ArrayList;

public class Visitor extends SysYBaseVisitor<Void> {
    //<editor-fold desc="Fields">

    public IRBuilder builder;
    public final Scope scope = new Scope();


    /**
     * Temporary variables for messages passed through layers visited.
     */
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

    @Override
    public Void visitCompUnit(SysYParser.CompUnitContext ctx) {
//        System.out.println("In CompUnit");
        super.visitCompUnit(ctx);
        return null;
    }


    //funcDef : funcType Identifier '(' (funcFParams)? ')' block
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
        Insert a terminator to the tail basic block of the function.
         */
        ArrayList<Instruction> instList = builder.getCurBB().instructions;
        Instruction retInst = (instList.size() == 0) ? null : instList.get(instList.size() - 1);
        // If no instruction in the bb, or the last instruction is not a terminator.
        if (retInst == null ||
                retInst.cat != Instruction.InstCategory.BR
                        && retInst.cat != Instruction.InstCategory.RET) {
            if (function.type instanceof FunctionType f) {
                if (f.getRetType().isVoidType()) {
                    builder.buildRet();
                }
                if (f.getRetType().isInteger()) {
                    // todo: return integer
                }
                // todo: return float
            }
        }

        return null;
    }


    @Override
    public Void visitFuncFParams(SysYParser.FuncFParamsContext ctx) {
        ArrayList<Type> retTypeList = new ArrayList<>();
        ctx.funcFParam().forEach(arg -> {
            visit(arg);
            retTypeList.add(tmpType);
        });
        return null;
    }


    @Override
    public Void visitFuncFParam(SysYParser.FuncFParamContext ctx) {
        // todo: Array as function arguments
        // todo: float as function arguments
        // Integer argument
        tmpType = IntegerType.getI32();
        return null;
    }

    @Override
    public Void visitBlock(SysYParser.BlockContext ctx) {
        scope.scopeIn();
        ctx.blockItem().forEach(this::visit);
        scope.scopeOut();
        return null;
    }

    //</editor-fold>
}
