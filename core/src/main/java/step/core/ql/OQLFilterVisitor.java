package step.core.ql;

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
			public boolean isValid(T input) {
				return left.isValid(input)&&right.isValid(input);
			}
        };
	}

	@Override
	public Filter<T> visitEqualityExpr(EqualityExprContext ctx) {
		return factory.createAttributeFilter(ctx.op.getText(), ctx.expr(0).getText(), ctx.expr(1).getText());
	}

	@Override
	public Filter<T> visitOrExpr(OrExprContext ctx) {
		final Filter<T> left = this.visit(ctx.expr(0));
		final Filter<T> right = this.visit(ctx.expr(1));
        return new Filter<T>() {
			@Override
			public boolean isValid(T input) {
				return left.isValid(input)||right.isValid(input);
			}
        };
	}

	@Override
	public Filter<T> visitNotExpr(NotExprContext ctx) {
		final Filter<T> expr = this.visit(ctx.expr());
        return new Filter<T>() {
			@Override
			public boolean isValid(T input) {
				return !expr.isValid(input);
			}
        };
	}

	@Override
	public Filter<T> visitParExpr(ParExprContext ctx) {
		final Filter<T> expr = this.visit(ctx.expr());
		return new Filter<T>() {
			@Override
			public boolean isValid(T input) {
				return expr.isValid(input);
			}
        };
	}

	@Override
	public Filter<T> visitNonQuotedStringAtom(NonQuotedStringAtomContext ctx) {
		return factory.createFullTextFilter(ctx.getText());
	}

	@Override
	public Filter<T> visitStringAtom(StringAtomContext ctx) {
		String str = ctx.getText();
        // strip quotes
        str = str.substring(1, str.length() - 1).replace("\"\"", "\"");
        return factory.createFullTextFilter(str);
	}


}
