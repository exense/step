// Generated from KeyValue.g4 by ANTLR 4.5.3

    package step.repositories.parser.keyvalue;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link KeyValueParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface KeyValueVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link KeyValueParser#parse}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParse(KeyValueParser.ParseContext ctx);
	/**
	 * Visit a parse tree produced by {@link KeyValueParser#keyValueList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeyValueList(KeyValueParser.KeyValueListContext ctx);
	/**
	 * Visit a parse tree produced by {@link KeyValueParser#keyValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeyValue(KeyValueParser.KeyValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link KeyValueParser#key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKey(KeyValueParser.KeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link KeyValueParser#value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue(KeyValueParser.ValueContext ctx);
}