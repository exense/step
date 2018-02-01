package step.core.dynamicbeans;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import step.expressions.ExpressionHandler;

public class DynamicValueResolverTest {

	@Test
	public void testString() {
		DynamicValue<String> v1 = new DynamicValue<>("'test'", "");
		DynamicValueResolver resolver = new DynamicValueResolver(new ExpressionHandler());
		resolver.evaluate(v1, null);
		Assert.assertEquals("test", v1.get());
	}
	
	@Test
	public void testString2() {
		DynamicValue<String> v1 = new DynamicValue<>("\"test\"", "");
		DynamicValueResolver resolver = new DynamicValueResolver(new ExpressionHandler());
		resolver.evaluate(v1, null);
		Assert.assertEquals("test", v1.get());
	}
	
	@Test
	public void testGString() {
		DynamicValue<String> v1 = new DynamicValue<>("\"te${'s'}t\"", "");
		DynamicValueResolver resolver = new DynamicValueResolver(new ExpressionHandler());
		resolver.evaluate(v1, null);
		Assert.assertEquals("test", v1.get());
	}
	
	@Test
	public void testGStringVariables() {
		DynamicValue<String> v1 = new DynamicValue<>("\"t${var}t\"", "");
		DynamicValueResolver resolver = new DynamicValueResolver(new ExpressionHandler());
		Map<String, Object> bindings = new HashMap<>();
		bindings.put("var", "es");
		resolver.evaluate(v1, bindings);
		Assert.assertEquals("test", v1.get());
	}
	
	@Test
	public void testInteger() {
		DynamicValue<Integer> v1 = new DynamicValue<>("1", "");
		DynamicValueResolver resolver = new DynamicValueResolver(new ExpressionHandler());
		resolver.evaluate(v1, null);
		Assert.assertEquals(1, (int)v1.get());
	}
	
	@Test
	public void testBoolean() {
		DynamicValue<Boolean> v1 = new DynamicValue<>("true", "");
		DynamicValueResolver resolver = new DynamicValueResolver(new ExpressionHandler());
		resolver.evaluate(v1, null);
		Assert.assertEquals(true, (boolean)v1.get());
	}
	
	@Test
	public void testJSONObject() {
		DynamicValue<JSONObject> v1 = new DynamicValue<>("new org.json.JSONObject(\"{'key1':'test'}\")", "");
		DynamicValueResolver resolver = new DynamicValueResolver(new ExpressionHandler());
		resolver.evaluate(v1, null);
		Assert.assertEquals(new JSONObject("{'key1':'test'}").get("key1"), v1.get().get("key1"));
	}
}
