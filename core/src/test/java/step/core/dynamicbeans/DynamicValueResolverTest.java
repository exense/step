package step.core.dynamicbeans;

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
	
	@Test
	public void testCastingError() {
		DynamicValue<String> v1 = new DynamicValue<String>("1", ""){};
		DynamicValueResolver resolver = new DynamicValueResolver(new ExpressionHandler());
		resolver.evaluate(v1, null);
		Assert.assertEquals("test", v1.get());
	}
}
