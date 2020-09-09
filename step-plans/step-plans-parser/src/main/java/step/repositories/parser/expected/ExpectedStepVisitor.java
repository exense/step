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
