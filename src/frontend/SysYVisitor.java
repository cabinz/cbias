// Generated from K:/Cbias/third-party/ANTLR\SysY.g4 by ANTLR 4.9.2
package frontend;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link SysYParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface SysYVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link SysYParser#compUnit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompUnit(SysYParser.CompUnitContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecl(SysYParser.DeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#constDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstDecl(SysYParser.ConstDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#bType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBType(SysYParser.BTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#constDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstDef(SysYParser.ConstDefContext ctx);
	/**
	 * Visit a parse tree produced by the {@code scalarConstInitVal}
	 * labeled alternative in {@link SysYParser#constInitVal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScalarConstInitVal(SysYParser.ScalarConstInitValContext ctx);
	/**
	 * Visit a parse tree produced by the {@code listConstInitVal}
	 * labeled alternative in {@link SysYParser#constInitVal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitListConstInitVal(SysYParser.ListConstInitValContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#varDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarDecl(SysYParser.VarDeclContext ctx);
	/**
	 * Visit a parse tree produced by the {@code varDefUninit}
	 * labeled alternative in {@link SysYParser#varDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarDefUninit(SysYParser.VarDefUninitContext ctx);
	/**
	 * Visit a parse tree produced by the {@code varDefInit}
	 * labeled alternative in {@link SysYParser#varDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarDefInit(SysYParser.VarDefInitContext ctx);
	/**
	 * Visit a parse tree produced by the {@code scalarInitVal}
	 * labeled alternative in {@link SysYParser#initVal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScalarInitVal(SysYParser.ScalarInitValContext ctx);
	/**
	 * Visit a parse tree produced by the {@code listInitval}
	 * labeled alternative in {@link SysYParser#initVal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitListInitval(SysYParser.ListInitvalContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#funcDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncDef(SysYParser.FuncDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#funcType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncType(SysYParser.FuncTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#funcFParams}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncFParams(SysYParser.FuncFParamsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#funcFParam}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncFParam(SysYParser.FuncFParamContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(SysYParser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#blockItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockItem(SysYParser.BlockItemContext ctx);
	/**
	 * Visit a parse tree produced by the {@code assignment}
	 * labeled alternative in {@link SysYParser#stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment(SysYParser.AssignmentContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exprStmt}
	 * labeled alternative in {@link SysYParser#stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprStmt(SysYParser.ExprStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code blkStmt}
	 * labeled alternative in {@link SysYParser#stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlkStmt(SysYParser.BlkStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ifStmt}
	 * labeled alternative in {@link SysYParser#stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfStmt(SysYParser.IfStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ifElseStmt}
	 * labeled alternative in {@link SysYParser#stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfElseStmt(SysYParser.IfElseStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code whileStmt}
	 * labeled alternative in {@link SysYParser#stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileStmt(SysYParser.WhileStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code breakStmt}
	 * labeled alternative in {@link SysYParser#stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBreakStmt(SysYParser.BreakStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code contStmt}
	 * labeled alternative in {@link SysYParser#stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContStmt(SysYParser.ContStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code retStmt}
	 * labeled alternative in {@link SysYParser#stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRetStmt(SysYParser.RetStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr(SysYParser.ExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#cond}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCond(SysYParser.CondContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#lVal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLVal(SysYParser.LValContext ctx);
	/**
	 * Visit a parse tree produced by the {@code primExpr1}
	 * labeled alternative in {@link SysYParser#primaryExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimExpr1(SysYParser.PrimExpr1Context ctx);
	/**
	 * Visit a parse tree produced by the {@code primExpr2}
	 * labeled alternative in {@link SysYParser#primaryExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimExpr2(SysYParser.PrimExpr2Context ctx);
	/**
	 * Visit a parse tree produced by the {@code primExpr3}
	 * labeled alternative in {@link SysYParser#primaryExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimExpr3(SysYParser.PrimExpr3Context ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumber(SysYParser.NumberContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#intConst}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntConst(SysYParser.IntConstContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#floatConst}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFloatConst(SysYParser.FloatConstContext ctx);
	/**
	 * Visit a parse tree produced by the {@code unary1}
	 * labeled alternative in {@link SysYParser#unaryExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnary1(SysYParser.Unary1Context ctx);
	/**
	 * Visit a parse tree produced by the {@code unary2}
	 * labeled alternative in {@link SysYParser#unaryExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnary2(SysYParser.Unary2Context ctx);
	/**
	 * Visit a parse tree produced by the {@code unary3}
	 * labeled alternative in {@link SysYParser#unaryExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnary3(SysYParser.Unary3Context ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#unaryOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryOp(SysYParser.UnaryOpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#funcRParams}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncRParams(SysYParser.FuncRParamsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code expAsRParam}
	 * labeled alternative in {@link SysYParser#funcRParam}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpAsRParam(SysYParser.ExpAsRParamContext ctx);
	/**
	 * Visit a parse tree produced by the {@code stringAsRParam}
	 * labeled alternative in {@link SysYParser#funcRParam}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringAsRParam(SysYParser.StringAsRParamContext ctx);
	/**
	 * Visit a parse tree produced by the {@code mul2}
	 * labeled alternative in {@link SysYParser#mulExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMul2(SysYParser.Mul2Context ctx);
	/**
	 * Visit a parse tree produced by the {@code mul1}
	 * labeled alternative in {@link SysYParser#mulExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMul1(SysYParser.Mul1Context ctx);
	/**
	 * Visit a parse tree produced by the {@code add2}
	 * labeled alternative in {@link SysYParser#addExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd2(SysYParser.Add2Context ctx);
	/**
	 * Visit a parse tree produced by the {@code add1}
	 * labeled alternative in {@link SysYParser#addExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd1(SysYParser.Add1Context ctx);
	/**
	 * Visit a parse tree produced by the {@code rel2}
	 * labeled alternative in {@link SysYParser#relExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRel2(SysYParser.Rel2Context ctx);
	/**
	 * Visit a parse tree produced by the {@code rel1}
	 * labeled alternative in {@link SysYParser#relExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRel1(SysYParser.Rel1Context ctx);
	/**
	 * Visit a parse tree produced by the {@code eq1}
	 * labeled alternative in {@link SysYParser#eqExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEq1(SysYParser.Eq1Context ctx);
	/**
	 * Visit a parse tree produced by the {@code eq2}
	 * labeled alternative in {@link SysYParser#eqExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEq2(SysYParser.Eq2Context ctx);
	/**
	 * Visit a parse tree produced by the {@code lAnd2}
	 * labeled alternative in {@link SysYParser#lAndExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLAnd2(SysYParser.LAnd2Context ctx);
	/**
	 * Visit a parse tree produced by the {@code lAnd1}
	 * labeled alternative in {@link SysYParser#lAndExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLAnd1(SysYParser.LAnd1Context ctx);
	/**
	 * Visit a parse tree produced by the {@code lOr1}
	 * labeled alternative in {@link SysYParser#lOrExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLOr1(SysYParser.LOr1Context ctx);
	/**
	 * Visit a parse tree produced by the {@code lOr2}
	 * labeled alternative in {@link SysYParser#lOrExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLOr2(SysYParser.LOr2Context ctx);
	/**
	 * Visit a parse tree produced by {@link SysYParser#constExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstExp(SysYParser.ConstExpContext ctx);
}