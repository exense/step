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
package step.core.ql;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import ch.exense.commons.core.collections.Filter;
import ch.exense.commons.core.collections.Filters;
import ch.exense.commons.core.collections.PojoFilter;
import ch.exense.commons.core.collections.PojoFilters.PojoFilterFactory;
import step.core.ql.OQLParser.ParseContext;

public class OQLFilterBuilder {
	
	public static Filter getFilter(String expression) {
		if(expression == null || expression.isEmpty()) {
			return Filters.empty();
		} else {
			ParseContext context = parse(expression);
			OQLFilterVisitor visitor = new OQLFilterVisitor();
			Filter filter = visitor.visit(context.getChild(0));
			return filter;
		}
	}
	
	public static PojoFilter<Object> getPojoFilter(String expression) {
		Filter filter = getFilter(expression);
		PojoFilter<Object> pojoFilter = new PojoFilterFactory<Object>().buildFilter(filter);
		return pojoFilter;
	}
	
	private static ParseContext parse(String expression) {
		OQLLexer lexer = new OQLLexer(new ANTLRInputStream(expression));
		OQLParser parser = new OQLParser(new CommonTokenStream(lexer));
		parser.addErrorListener(new BaseErrorListener() {
	        @Override
	        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
	            throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
	        }
	    });
		return parser.parse();
		
	}
}
