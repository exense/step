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

import step.core.ql.Filter;
import step.core.ql.FilterFactory;
import step.core.ql.OQLBaseVisitor;
import step.core.ql.OQLParser.AndExprContext;
import step.core.ql.OQLParser.EqualityExprContext;
import step.core.ql.OQLParser.NonQuotedStringAtomContext;
import step.core.ql.OQLParser.NotExprContext;
import step.core.ql.OQLParser.OrExprContext;
import step.core.ql.OQLParser.ParExprContext;
import step.core.ql.OQLParser.StringAtomContext;

public class OQLFilterVisitor <T> extends OQLBaseVisitor<Filter<T>>{

	private FilterFactory<T> factory;

	public OQLFilterVisitor(FilterFactory<T> factory) {
		super();
		this.factory = factory;
	}

	@Override
	public Filter<T> visitAndExpr(AndExprContext ctx) {
		final Filter<T> left = this.visit(ctx.expr(0));
		final Filter<T> right = this.visit(ctx.expr(1));
        return new Filter<T>() {
			@Override
			public boolean test(T input) {
				return left.test(input)&&right.test(input);
			}
        };
	}

	@Override
	public Filter<T> visitEqualityExpr(EqualityExprContext ctx) {
		String text0 = unescapeStringIfNecessary(ctx.expr(0).getText());
		String text1 = unescapeStringIfNecessary(ctx.expr(1).getText());
		return factory.createAttributeFilter(ctx.op.getText(), text0, text1);
	}

	protected String unescapeStringIfNecessary(String text1) {
		if(text1.startsWith("\"") && text1.endsWith("\"")) {
			text1 = unescapeStringAtom(text1);
		}
		return text1;
	}

	@Override
	public Filter<T> visitOrExpr(OrExprContext ctx) {
		final Filter<T> left = this.visit(ctx.expr(0));
		final Filter<T> right = this.visit(ctx.expr(1));
        return new Filter<T>() {
			@Override
			public boolean test(T input) {
				return left.test(input)||right.test(input);
			}
        };
	}

	@Override
	public Filter<T> visitNotExpr(NotExprContext ctx) {
		final Filter<T> expr = this.visit(ctx.expr());
        return new Filter<T>() {
			@Override
			public boolean test(T input) {
				return !expr.test(input);
			}
        };
	}

	@Override
	public Filter<T> visitParExpr(ParExprContext ctx) {
		final Filter<T> expr = this.visit(ctx.expr());
		return new Filter<T>() {
			@Override
			public boolean test(T input) {
				return expr.test(input);
			}
        };
	}

	@Override
	public Filter<T> visitNonQuotedStringAtom(NonQuotedStringAtomContext ctx) {
		return factory.createFullTextFilter(ctx.getText());
	}

	@Override
	public Filter<T> visitStringAtom(StringAtomContext ctx) {
		String str = unescapeStringAtom(ctx.getText());
        return factory.createFullTextFilter(str);
	}

	protected String unescapeStringAtom(String str) {
        // strip quotes
        str = str.substring(1, str.length() - 1).replace("\"\"", "\"");
		return str;
	}


}
