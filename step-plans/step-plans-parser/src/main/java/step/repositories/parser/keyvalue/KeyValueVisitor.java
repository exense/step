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
