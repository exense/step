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

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.not;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Filters.regex;

import org.bson.Document;
import org.bson.conversions.Bson;

import step.core.ql.OQLBaseVisitor;
import step.core.ql.OQLParser.AndExprContext;
import step.core.ql.OQLParser.EqualityExprContext;
import step.core.ql.OQLParser.NonQuotedStringAtomContext;
import step.core.ql.OQLParser.NotExprContext;
import step.core.ql.OQLParser.OrExprContext;
import step.core.ql.OQLParser.ParExprContext;
import step.core.ql.OQLParser.StringAtomContext;

public class OQLMongoDBQueryVisitor extends OQLBaseVisitor<Bson>{

	StringBuilder builder = new StringBuilder();

	public OQLMongoDBQueryVisitor() {
		super();
	}

	@Override
	public Bson visitAndExpr(AndExprContext ctx) {
		return and(this.visit(ctx.expr(0)), this.visit(ctx.expr(1)));
	}

	@Override
	public Bson visitEqualityExpr(EqualityExprContext ctx) {
		String op = ctx.op.getText();
		
		String value = ctx.expr(1).getText();
		if(ctx.expr(1).getChildCount()==1&&ctx.expr(1).getChild(0) instanceof StringAtomContext) {
			value = value.substring(1, value.length()-1);
			// strip quotes
			value = value.replace("\"\"", "\"");
		}
		
		if(op.equals("=")) 
    		return new Document(ctx.expr(0).getText(), value);
    	else if (op.equals("~"))
    		return regex(ctx.expr(0).getText(), value); 
    	else 
    		throw new RuntimeException("Invalid operator: '"+op+"'");
	}

	@Override
	public Bson visitOrExpr(OrExprContext ctx) {
		return or(this.visit(ctx.expr(0)), this.visit(ctx.expr(1)));
	}

	@Override
	public Bson visitNotExpr(NotExprContext ctx) {
		return not(this.visit(ctx.expr()));
	}

	@Override
	public Bson visitParExpr(ParExprContext ctx) {
		return this.visit(ctx.expr());
	}

	@Override
	public Bson visitNonQuotedStringAtom(NonQuotedStringAtomContext ctx) {
		throw new RuntimeException("Missing assignment");
	}

	@Override
	public Bson visitStringAtom(StringAtomContext ctx) {
		throw new RuntimeException("Missing assignment");
	}
}

