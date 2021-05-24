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

import java.util.List;

import ch.exense.commons.core.collections.Filter;
import ch.exense.commons.core.collections.Filters;
import step.core.ql.OQLParser.AndExprContext;
import step.core.ql.OQLParser.EqualityExprContext;
import step.core.ql.OQLParser.NonQuotedStringAtomContext;
import step.core.ql.OQLParser.NotExprContext;
import step.core.ql.OQLParser.OrExprContext;
import step.core.ql.OQLParser.ParExprContext;
import step.core.ql.OQLParser.StringAtomContext;

public class OQLFilterVisitor extends OQLBaseVisitor<Filter>{

	public OQLFilterVisitor() {
		super();
	}

	@Override
	public Filter visitAndExpr(AndExprContext ctx) {
		final Filter left = this.visit(ctx.expr(0));
		final Filter right = this.visit(ctx.expr(1));
        return Filters.and(List.of(left, right));
	}

	@Override
	public Filter visitEqualityExpr(EqualityExprContext ctx) {
		String text0 = unescapeStringIfNecessary(ctx.expr(0).getText());
		String text1 = unescapeStringIfNecessary(ctx.expr(1).getText());
		return Filters.equals(text0, text1);
	}

	protected String unescapeStringIfNecessary(String text1) {
		if(text1.startsWith("\"") && text1.endsWith("\"")) {
			text1 = unescapeStringAtom(text1);
		}
		return text1;
	}

	@Override
	public Filter visitOrExpr(OrExprContext ctx) {
		final Filter left = this.visit(ctx.expr(0));
		final Filter right = this.visit(ctx.expr(1));
        return Filters.or(List.of(left, right));
	}

	@Override
	public Filter visitNotExpr(NotExprContext ctx) {
		final Filter expr = this.visit(ctx.expr());
        return Filters.not(expr);
	}

	@Override
	public Filter visitParExpr(ParExprContext ctx) {
		final Filter expr = this.visit(ctx.expr());
		return expr;
	}

	@Override
	public Filter visitNonQuotedStringAtom(NonQuotedStringAtomContext ctx) {
		return Filters.fulltext(ctx.getText());
	}

	@Override
	public Filter visitStringAtom(StringAtomContext ctx) {
		String str = unescapeStringAtom(ctx.getText());
        return Filters.fulltext(str);
	}

	protected String unescapeStringAtom(String str) {
        // strip quotes
        str = str.substring(1, str.length() - 1).replace("\"\"", "\"");
		return str;
	}


}
