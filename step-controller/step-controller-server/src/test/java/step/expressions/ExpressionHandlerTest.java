package step.expressions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import junit.framework.Assert;

public class ExpressionHandlerTest {

	@Test
	public void testDefault() {
		ExpressionHandler e = new ExpressionHandler();
		Object o = e.evaluateGroovyExpression("1+1", null);
		Assert.assertEquals(2,o);
	}
	
	@Test
	public void testBindings() {
		ExpressionHandler e = new ExpressionHandler("step.expressions.GroovyFunctions");
		Map<String, Object> b = new HashMap<>();
		b.put("test", "value");
		Object o = e.evaluateGroovyExpression("test", b);
		Assert.assertEquals("value", o.toString());
	}
	
	@Test
	public void testScriptBaseClass() {
		ExpressionHandler e = new ExpressionHandler("step.expressions.GroovyFunctions");
		Object o = e.evaluateGroovyExpression("yyyyMMdd", null);
		SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");
		Assert.assertEquals(f.format(new Date()), o.toString());
	}

	@Test
	public void testScriptBaseClassWithArrays() {
		ExpressionHandler e = new ExpressionHandler("step.expressions.GroovyTestFunctions");
		Object o = e.evaluateGroovyExpression("\"${testArrays()[0]}\"", null);
		Assert.assertEquals("foo", o.toString());
	}
}
