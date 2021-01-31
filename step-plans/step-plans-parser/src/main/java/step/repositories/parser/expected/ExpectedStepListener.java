// Generated from ExpectedStep.g4 by ANTLR 4.5.3

    package step.repositories.parser.expected;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ExpectedStepParser}.
 */
public interface ExpectedStepListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ExpectedStepParser#parse}.
	 * @param ctx the parse tree
	 */
	void enterParse(ExpectedStepParser.ParseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpectedStepParser#parse}.
	 * @param ctx the parse tree
	 */
	void exitParse(ExpectedStepParser.ParseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpectedStepParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(ExpectedStepParser.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpectedStepParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(ExpectedStepParser.ExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code checkExpr}
	 * labeled alternative in {@link ExpectedStepParser#checkExpression}.
	 * @param ctx the parse tree
	 */
	void enterCheckExpr(ExpectedStepParser.CheckExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code checkExpr}
	 * labeled alternative in {@link ExpectedStepParser#checkExpression}.
	 * @param ctx the parse tree
	 */
	void exitCheckExpr(ExpectedStepParser.CheckExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpectedStepParser#setExpression}.
	 * @param ctx the parse tree
	 */
	void enterSetExpression(ExpectedStepParser.SetExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpectedStepParser#setExpression}.
	 * @param ctx the parse tree
	 */
	void exitSetExpression(ExpectedStepParser.SetExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpectedStepParser#assignment}.
	 * @param ctx the parse tree
	 */
	void enterAssignment(ExpectedStepParser.AssignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpectedStepParser#assignment}.
	 * @param ctx the parse tree
	 */
	void exitAssignment(ExpectedStepParser.AssignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpectedStepParser#exportExpression}.
	 * @param ctx the parse tree
	 */
	void enterExportExpression(ExpectedStepParser.ExportExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpectedStepParser#exportExpression}.
	 * @param ctx the parse tree
	 */
	void exitExportExpression(ExpectedStepParser.ExportExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpectedStepParser#controlParameter}.
	 * @param ctx the parse tree
	 */
	void enterControlParameter(ExpectedStepParser.ControlParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpectedStepParser#controlParameter}.
	 * @param ctx the parse tree
	 */
	void exitControlParameter(ExpectedStepParser.ControlParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpectedStepParser#outputAttributeName}.
	 * @param ctx the parse tree
	 */
	void enterOutputAttributeName(ExpectedStepParser.OutputAttributeNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpectedStepParser#outputAttributeName}.
	 * @param ctx the parse tree
	 */
	void exitOutputAttributeName(ExpectedStepParser.OutputAttributeNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpectedStepParser#attributeName}.
	 * @param ctx the parse tree
	 */
	void enterAttributeName(ExpectedStepParser.AttributeNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpectedStepParser#attributeName}.
	 * @param ctx the parse tree
	 */
	void exitAttributeName(ExpectedStepParser.AttributeNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpectedStepParser#setValue}.
	 * @param ctx the parse tree
	 */
	void enterSetValue(ExpectedStepParser.SetValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpectedStepParser#setValue}.
	 * @param ctx the parse tree
	 */
	void exitSetValue(ExpectedStepParser.SetValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ExpectedStepParser#attributeValue}.
	 * @param ctx the parse tree
	 */
	void enterAttributeValue(ExpectedStepParser.AttributeValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ExpectedStepParser#attributeValue}.
	 * @param ctx the parse tree
	 */
	void exitAttributeValue(ExpectedStepParser.AttributeValueContext ctx);
}