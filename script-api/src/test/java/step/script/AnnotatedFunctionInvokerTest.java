package step.script;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import step.script.AnnotatedMethodInvoker;
import step.script.Arg;
import step.script.Prop;

public class AnnotatedFunctionInvokerTest {

	@Test
	public void test() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException {
		String val = "TEST";
		Object result = AnnotatedMethodInvoker.invoke(this, AnnotatedFunctionInvokerTest.class.getMethod("testMethod",String.class), Json.createObjectBuilder().add("val1", val).build().toString());
		
		Assert.assertEquals(val, result);
	}
	
	@Test
	public void testProp() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException {
		String val = "TEST";
		Map<String, String> properties = new HashMap<>();
		properties.put("prop1", val);
		Object result = AnnotatedMethodInvoker.invoke(this, AnnotatedFunctionInvokerTest.class.getMethod("testMethodProp",String.class), null, properties);
		
		Assert.assertEquals(val, result);
	}
	
	@Test
	public void testVoid() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException {
		String val = "TEST";
		Object result = AnnotatedMethodInvoker.invoke(this, AnnotatedFunctionInvokerTest.class.getMethod("testMethodVoid",String.class), Json.createObjectBuilder().add("val1", val).build().toString());
		
		Assert.assertNull(result);
	}
	
	@Test
	public void testNotAnnotated() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException {
		String val = "TEST";
		Object result = AnnotatedMethodInvoker.invoke(this, AnnotatedFunctionInvokerTest.class.getMethod("testMethodNotAnnotated",String.class,String.class), Json.createObjectBuilder().add("val1", val).build().toString());
		Assert.assertEquals(val, result);
	}
	
	@Test
	public void testInteger() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Integer val = 10;
		Object result = AnnotatedMethodInvoker.invoke(this, AnnotatedFunctionInvokerTest.class.getMethod("testMethod",Integer.class), Json.createObjectBuilder().add("val1", val).build().toString());
		Assert.assertEquals(val, result);
	}
	
	public Object testMethod(@Arg("$.val1") String val1) {
		return val1;		
	}
	
	public Object testMethod(@Arg("val1") Integer val1) {
		return val1;		
	}
	
	public Object testMethodNotAnnotated(@Arg("$.val1") String val1, String notAnnotated) {
		return val1;		
	}
	
	public Object testMethod2(@Arg("$.val1") String val1) {
		return val1;		
	}
	
	public void testMethodVoid(@Arg("$.val1") String val1) {
				
	}
	
	public Object testMethod(@Arg("$") JsonObject val1) {
		return val1;		
	}

	public Object testMethodProp(@Prop("prop1") String prop1) {
		return prop1;		
	}
}
