package step.core.ql;

import java.lang.reflect.InvocationTargetException;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.beanutils.PropertyUtils;

import step.core.ql.Filter;
import step.core.ql.FilterFactory;
import step.core.ql.OQLFilterVisitor;
import step.core.ql.OQLLexer;
import step.core.ql.OQLParser;
import step.core.ql.OQLParser.ParseContext;

public class OQLFilterBuilder {

	public static Filter<Object> getFilter(String expression) {
		return getFilter(expression, new FilterFactory<Object>() {
			@Override
			public Filter<Object> createFullTextFilter(String expression) {
				throw new RuntimeException("Full text search not implemented");
			}

			@Override
			public Filter<Object> createAttributeFilter(String operator, String attribute, String value) {
				return new Filter<Object>() {
					@Override
					public boolean test(Object input) {
						try {
							return value.equals(PropertyUtils.getProperty(input, attribute));
						} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | NestedNullException e) {
							return false;
						}
					}
				};
			}
		});
	}
	
	public static <T> Filter<T>  getFilter(String expression, FilterFactory<T> factory) {
		if(expression == null || expression.isEmpty()) {
			return new Filter<T>() {
				@Override
				public boolean test(T t) {
					return true;
				}
			};
		} else {
			ParseContext context = parse(expression);
			OQLFilterVisitor<T> visitor = new OQLFilterVisitor<>(factory);
			Filter<T> filter = visitor.visit(context.getChild(0));
			return filter;
		}
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
