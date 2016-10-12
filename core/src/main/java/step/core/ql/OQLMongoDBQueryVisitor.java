package step.core.ql;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.not;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Filters.regex;

import org.bson.Document;
import org.bson.conversions.Bson;

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

