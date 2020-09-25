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
// Generated from OQL.g4 by ANTLR 4.5.3

    package step.core.ql;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link OQLParser}.
 */
public interface OQLListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link OQLParser#parse}.
	 * @param ctx the parse tree
	 */
	void enterParse(OQLParser.ParseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OQLParser#parse}.
	 * @param ctx the parse tree
	 */
	void exitParse(OQLParser.ParseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code notExpr}
	 * labeled alternative in {@link OQLParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterNotExpr(OQLParser.NotExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code notExpr}
	 * labeled alternative in {@link OQLParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitNotExpr(OQLParser.NotExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code atomExpr}
	 * labeled alternative in {@link OQLParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterAtomExpr(OQLParser.AtomExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code atomExpr}
	 * labeled alternative in {@link OQLParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitAtomExpr(OQLParser.AtomExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code orExpr}
	 * labeled alternative in {@link OQLParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterOrExpr(OQLParser.OrExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code orExpr}
	 * labeled alternative in {@link OQLParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitOrExpr(OQLParser.OrExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code equalityExpr}
	 * labeled alternative in {@link OQLParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterEqualityExpr(OQLParser.EqualityExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code equalityExpr}
	 * labeled alternative in {@link OQLParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitEqualityExpr(OQLParser.EqualityExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code andExpr}
	 * labeled alternative in {@link OQLParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterAndExpr(OQLParser.AndExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code andExpr}
	 * labeled alternative in {@link OQLParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitAndExpr(OQLParser.AndExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parExpr}
	 * labeled alternative in {@link OQLParser#atom}.
	 * @param ctx the parse tree
	 */
	void enterParExpr(OQLParser.ParExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parExpr}
	 * labeled alternative in {@link OQLParser#atom}.
	 * @param ctx the parse tree
	 */
	void exitParExpr(OQLParser.ParExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nonQuotedStringAtom}
	 * labeled alternative in {@link OQLParser#atom}.
	 * @param ctx the parse tree
	 */
	void enterNonQuotedStringAtom(OQLParser.NonQuotedStringAtomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nonQuotedStringAtom}
	 * labeled alternative in {@link OQLParser#atom}.
	 * @param ctx the parse tree
	 */
	void exitNonQuotedStringAtom(OQLParser.NonQuotedStringAtomContext ctx);
	/**
	 * Enter a parse tree produced by the {@code stringAtom}
	 * labeled alternative in {@link OQLParser#atom}.
	 * @param ctx the parse tree
	 */
	void enterStringAtom(OQLParser.StringAtomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code stringAtom}
	 * labeled alternative in {@link OQLParser#atom}.
	 * @param ctx the parse tree
	 */
	void exitStringAtom(OQLParser.StringAtomContext ctx);
}
