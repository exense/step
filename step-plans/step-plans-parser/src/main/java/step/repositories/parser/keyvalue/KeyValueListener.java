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
