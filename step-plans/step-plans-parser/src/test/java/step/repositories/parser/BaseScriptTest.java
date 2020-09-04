package step.repositories.parser;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import step.expressions.ExpressionHandler;

public class BaseScriptTest {
	
	ExpressionHandler h = new ExpressionHandler("step.repositories.parser.BaseScript");

	@Test
	public void test() {
		Object s = h.evaluateGroovyExpression("heute()", null);
		Assert.assertEquals(format(new Date(), "dd.MM.yyyy"), s);
		
		s = h.evaluateGroovyExpression("jetzt()", null);
		Assert.assertEquals(format(new Date(), "dd.MM.yyyy HH:mm:ss"), s);
		
		s = h.evaluateGroovyExpression("datum('1.minute.from.now')", null);
		
		s = h.evaluateGroovyExpression("letzterTagImMonat()", null);
		s = h.evaluateGroovyExpression("letzterTagImMonat('dd')", null);
		
		s = h.evaluateGroovyExpression("ersterTagNaechsterMonat('dd.MM.yyyy')", null);
		
		s = h.evaluateGroovyExpression("ersterTagImMonat('dd.MM.yyyy')", null);
	}
	
	private String format(Date date, String format) {
		SimpleDateFormat f = new SimpleDateFormat(format);
		return f.format(date);

	}
}
