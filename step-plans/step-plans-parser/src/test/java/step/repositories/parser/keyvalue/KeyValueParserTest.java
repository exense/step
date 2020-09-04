package step.repositories.parser.keyvalue;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Assert;
import org.junit.Test;

import step.repositories.parser.keyvalue.KeyValueParser.KeyValueContext;
import step.repositories.parser.keyvalue.KeyValueParser.ParseContext;

public class KeyValueParserTest {

	@Test
	public void test() {
		StringBuilder result = new StringBuilder();
		StringBuilder dynamicExpressions = new StringBuilder();

		KeyValueParser p = new KeyValueParser(new CommonTokenStream(new KeyValueLexer(new ANTLRInputStream("k1 = | a||b | k2 = \"myString \"    k3=myVar k4=|'test'|"))));
		
		ParseContext context = p.parse();
		KeyValueVisitor<Object> visitor = new KeyValueBaseVisitor<Object>() {

			@Override
			public Object visitKeyValue(KeyValueContext ctx) {
				result.append(ctx.value().getText());
				
				if(ctx.value().DYNAMIC_EXPRESSION()!=null) {
					dynamicExpressions.append(ctx.value().DYNAMIC_EXPRESSION().getText()).append(";");
				}
				
				return super.visitKeyValue(ctx);
			}
		};
		visitor.visit(context);
		
		p.parse();
		
		Assert.assertEquals("| a||b |\"myString \"myVar|'test'|", result.toString());
		Assert.assertEquals("| a||b |;|'test'|;", dynamicExpressions.toString());

	}

}
