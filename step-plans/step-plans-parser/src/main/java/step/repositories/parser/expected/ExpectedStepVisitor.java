// Generated from ExpectedStep.g4 by ANTLR 4.5.3

    package step.repositories.parser.expected;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link ExpectedStepParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface ExpectedStepVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link ExpectedStepParser#parse}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParse(ExpectedStepParser.ParseContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpectedStepParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr(ExpectedStepParser.ExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code checkExpr}
	 * labeled alternative in {@link ExpectedStepParser#checkExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCheckExpr(ExpectedStepParser.CheckExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpectedStepParser#setExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetExpression(ExpectedStepParser.SetExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpectedStepParser#assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment(ExpectedStepParser.AssignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpectedStepParser#exportExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExportExpression(ExpectedStepParser.ExportExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpectedStepParser#controlParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitControlParameter(ExpectedStepParser.ControlParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpectedStepParser#attributeName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttributeName(ExpectedStepParser.AttributeNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpectedStepParser#setValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetValue(ExpectedStepParser.SetValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link ExpectedStepParser#attributeValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttributeValue(ExpectedStepParser.AttributeValueContext ctx);
}