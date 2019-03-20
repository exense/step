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