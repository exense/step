package step.expressions;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import junit.framework.Assert;

public class ExpressionHandlerTest {

	@Test
	public void test() {
		ExpressionHandler e = new ExpressionHandler();
		Assert.assertEquals("abc",e.evaluate("ab[['c']]", null));
	}
	
	@Test
	public void test2() {
		ExpressionHandler e = new ExpressionHandler();
		Assert.assertEquals("abcd",e.evaluate("ab[['c']][['d']]", null));
	}
	
	@Test
	public void test3() {
		ExpressionHandler e = new ExpressionHandler();
		Assert.assertEquals("abcde",e.evaluate("ab[['c']][['d']]e", null));
	}
	
	@Test
	public void test4() {
		ExpressionHandler e = new ExpressionHandler();
		Assert.assertEquals("abcd",e.evaluate("ab[[if(true){return 'c'}]]d", null));
	}
	
	@Test
	public void test5() {
		Map<String, Object> o = new HashMap<>();
		o.put("var", "c");
		ExpressionHandler e = new ExpressionHandler();
		Assert.assertEquals("abcd",e.evaluate("ab[[var]]d", o));
	}

}
