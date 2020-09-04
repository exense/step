// Generated from DescriptionStep.g4 by ANTLR 4.5.3

    package step.repositories.parser.description;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link DescriptionStepParser}.
 */
public interface DescriptionStepListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link DescriptionStepParser#parse}.
	 * @param ctx the parse tree
	 */
	void enterParse(DescriptionStepParser.ParseContext ctx);
	/**
	 * Exit a parse tree produced by {@link DescriptionStepParser#parse}.
	 * @param ctx the parse tree
	 */
	void exitParse(DescriptionStepParser.ParseContext ctx);
	/**
	 * Enter a parse tree produced by {@link DescriptionStepParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(DescriptionStepParser.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link DescriptionStepParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(DescriptionStepParser.ExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link DescriptionStepParser#keywordExpression}.
	 * @param ctx the parse tree
	 */
	void enterKeywordExpression(DescriptionStepParser.KeywordExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DescriptionStepParser#keywordExpression}.
	 * @param ctx the parse tree
	 */
	void exitKeywordExpression(DescriptionStepParser.KeywordExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DescriptionStepParser#setExpression}.
	 * @param ctx the parse tree
	 */
	void enterSetExpression(DescriptionStepParser.SetExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DescriptionStepParser#setExpression}.
	 * @param ctx the parse tree
	 */
	void exitSetExpression(DescriptionStepParser.SetExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DescriptionStepParser#functionDeclarationExpression}.
	 * @param ctx the parse tree
	 */
	void enterFunctionDeclarationExpression(DescriptionStepParser.FunctionDeclarationExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DescriptionStepParser#functionDeclarationExpression}.
	 * @param ctx the parse tree
	 */
	void exitFunctionDeclarationExpression(DescriptionStepParser.FunctionDeclarationExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DescriptionStepParser#functionDeclarationEndExpression}.
	 * @param ctx the parse tree
	 */
	void enterFunctionDeclarationEndExpression(DescriptionStepParser.FunctionDeclarationEndExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DescriptionStepParser#functionDeclarationEndExpression}.
	 * @param ctx the parse tree
	 */
	void exitFunctionDeclarationEndExpression(DescriptionStepParser.FunctionDeclarationEndExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DescriptionStepParser#assignment}.
	 * @param ctx the parse tree
	 */
	void enterAssignment(DescriptionStepParser.AssignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link DescriptionStepParser#assignment}.
	 * @param ctx the parse tree
	 */
	void exitAssignment(DescriptionStepParser.AssignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link DescriptionStepParser#keywordParameter}.
	 * @param ctx the parse tree
	 */
	void enterKeywordParameter(DescriptionStepParser.KeywordParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link DescriptionStepParser#keywordParameter}.
	 * @param ctx the parse tree
	 */
	void exitKeywordParameter(DescriptionStepParser.KeywordParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link DescriptionStepParser#keywordName}.
	 * @param ctx the parse tree
	 */
	void enterKeywordName(DescriptionStepParser.KeywordNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link DescriptionStepParser#keywordName}.
	 * @param ctx the parse tree
	 */
	void exitKeywordName(DescriptionStepParser.KeywordNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link DescriptionStepParser#attributeName}.
	 * @param ctx the parse tree
	 */
	void enterAttributeName(DescriptionStepParser.AttributeNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link DescriptionStepParser#attributeName}.
	 * @param ctx the parse tree
	 */
	void exitAttributeName(DescriptionStepParser.AttributeNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link DescriptionStepParser#attributeValue}.
	 * @param ctx the parse tree
	 */
	void enterAttributeValue(DescriptionStepParser.AttributeValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link DescriptionStepParser#attributeValue}.
	 * @param ctx the parse tree
	 */
	void exitAttributeValue(DescriptionStepParser.AttributeValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link DescriptionStepParser#setValue}.
	 * @param ctx the parse tree
	 */
	void enterSetValue(DescriptionStepParser.SetValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link DescriptionStepParser#setValue}.
	 * @param ctx the parse tree
	 */
	void exitSetValue(DescriptionStepParser.SetValueContext ctx);
}