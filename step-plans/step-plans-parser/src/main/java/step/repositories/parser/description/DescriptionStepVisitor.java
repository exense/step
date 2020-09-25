/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
// Generated from DescriptionStep.g4 by ANTLR 4.5.3

    package step.repositories.parser.description;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link DescriptionStepParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface DescriptionStepVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link DescriptionStepParser#parse}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParse(DescriptionStepParser.ParseContext ctx);
	/**
	 * Visit a parse tree produced by {@link DescriptionStepParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr(DescriptionStepParser.ExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link DescriptionStepParser#keywordExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeywordExpression(DescriptionStepParser.KeywordExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DescriptionStepParser#setExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetExpression(DescriptionStepParser.SetExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DescriptionStepParser#functionDeclarationExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionDeclarationExpression(DescriptionStepParser.FunctionDeclarationExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DescriptionStepParser#functionDeclarationEndExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionDeclarationEndExpression(DescriptionStepParser.FunctionDeclarationEndExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DescriptionStepParser#assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment(DescriptionStepParser.AssignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link DescriptionStepParser#keywordParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeywordParameter(DescriptionStepParser.KeywordParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link DescriptionStepParser#keywordName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeywordName(DescriptionStepParser.KeywordNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link DescriptionStepParser#attributeName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttributeName(DescriptionStepParser.AttributeNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link DescriptionStepParser#attributeValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttributeValue(DescriptionStepParser.AttributeValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link DescriptionStepParser#setValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetValue(DescriptionStepParser.SetValueContext ctx);
}
