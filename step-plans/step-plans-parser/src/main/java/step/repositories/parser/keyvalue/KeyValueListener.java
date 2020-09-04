// Generated from KeyValue.g4 by ANTLR 4.5.3

    package step.repositories.parser.keyvalue;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link KeyValueParser}.
 */
public interface KeyValueListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link KeyValueParser#parse}.
	 * @param ctx the parse tree
	 */
	void enterParse(KeyValueParser.ParseContext ctx);
	/**
	 * Exit a parse tree produced by {@link KeyValueParser#parse}.
	 * @param ctx the parse tree
	 */
	void exitParse(KeyValueParser.ParseContext ctx);
	/**
	 * Enter a parse tree produced by {@link KeyValueParser#keyValueList}.
	 * @param ctx the parse tree
	 */
	void enterKeyValueList(KeyValueParser.KeyValueListContext ctx);
	/**
	 * Exit a parse tree produced by {@link KeyValueParser#keyValueList}.
	 * @param ctx the parse tree
	 */
	void exitKeyValueList(KeyValueParser.KeyValueListContext ctx);
	/**
	 * Enter a parse tree produced by {@link KeyValueParser#keyValue}.
	 * @param ctx the parse tree
	 */
	void enterKeyValue(KeyValueParser.KeyValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link KeyValueParser#keyValue}.
	 * @param ctx the parse tree
	 */
	void exitKeyValue(KeyValueParser.KeyValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link KeyValueParser#key}.
	 * @param ctx the parse tree
	 */
	void enterKey(KeyValueParser.KeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link KeyValueParser#key}.
	 * @param ctx the parse tree
	 */
	void exitKey(KeyValueParser.KeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link KeyValueParser#value}.
	 * @param ctx the parse tree
	 */
	void enterValue(KeyValueParser.ValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link KeyValueParser#value}.
	 * @param ctx the parse tree
	 */
	void exitValue(KeyValueParser.ValueContext ctx);
}