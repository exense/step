package step.repositories.parser.annotated;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import step.repositories.parser.keyvalue.KeyValueBaseVisitor;
import step.repositories.parser.keyvalue.KeyValueLexer;
import step.repositories.parser.keyvalue.KeyValueParser;
import step.repositories.parser.keyvalue.KeyValueParser.KeyValueContext;
import step.repositories.parser.keyvalue.KeyValueParser.ParseContext;
import step.repositories.parser.keyvalue.KeyValueVisitor;
import step.repositories.parser.steps.DescriptionStep;

public class AbstractDescriptionStepParser extends AnnotatedStepParser<DescriptionStep> {

	protected static JsonObject parseKeyValues(String selectionCriteriaExpr) {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		KeyValueParser parser = new KeyValueParser(new CommonTokenStream(new KeyValueLexer(new ANTLRInputStream(selectionCriteriaExpr))));
		parser.addErrorListener(new BaseErrorListener() {
	        @Override
	        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
	            throw new IllegalStateException(msg, e);
	        }
	    });
		ParseContext context = parser.parse();
		KeyValueVisitor<Object> visitor = new KeyValueBaseVisitor<Object>() {
	
			@Override
			public Object visitKeyValue(KeyValueContext ctx) {
				String key;
				if(ctx.key().WORD()!=null) {
					key = ctx.key().WORD().getText();
				} else {
					String litteral = ctx.key().STRING().getText();
					key = litteral.substring(1, litteral.length()-1);
				}
				String value = ctx.value().getText();
				String expression;
				if(ctx.value().DYNAMIC_EXPRESSION()!=null) {
					String dynamicExpressionText = ctx.value().DYNAMIC_EXPRESSION().getText();
					expression = dynamicExpressionText.substring(1, dynamicExpressionText.length()-1);
					expression.replace("||", "|");
				} else {
					expression = value;
				}
				
				JsonObjectBuilder dynamicAttribute = Json.createObjectBuilder();;
				dynamicAttribute.add("dynamic", true);
				dynamicAttribute.add("expression", expression);
				
				builder.add(key, dynamicAttribute.build());
				return super.visitKeyValue(ctx);
			}
		};
		visitor.visit(context);
		JsonObject object = builder.build();
		return object;
	}

	public AbstractDescriptionStepParser(Class<DescriptionStep> stepClass) {
		super(stepClass);
	}

}