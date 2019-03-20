package step.core.dynamicbeans;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import step.expressions.ExpressionHandler;

public class DynamicBeanResolverTest {

	DynamicBeanResolver resolver = new DynamicBeanResolver(new DynamicValueResolver(new ExpressionHandler()));
	
	@Test
	public void test1() {
		TestBean bean = new TestBean();
		resolver.evaluate(bean, null);
		Assert.assertEquals("test", bean.getTestString().get());
		Assert.assertEquals("test", bean.getTestRecursive().get().getTestString().get());
		Assert.assertEquals("test", bean.getTestRecursive2().getTestString().get());
	}
	
	@Test
	public void testClone() {
		TestBean bean = new TestBean();
		bean.testBoolean.setExpression("boolean1");
		bean.getTestRecursive2().getTestString().setExpression("string1");;
		
		
		TestBean bean1 = resolver.cloneDynamicValues(bean);	
		Map<String, Object> b = new HashMap<>();
		b.put("boolean1", true);
		b.put("string1", "str1");
		resolver.evaluate(bean1, b);
		
		TestBean bean2 = resolver.cloneDynamicValues(bean);	
		b = new HashMap<>();
		b.put("boolean1", false);
		b.put("string1", "str2");
		resolver.evaluate(bean2, b);

		Assert.assertEquals(true, bean1.getTestBoolean().get());
		Assert.assertEquals(false, bean2.getTestBoolean().get());
		// Testing the recursive cloning of dynamic values
		Assert.assertEquals("str1", bean1.getTestRecursive2().getTestString().get());
		Assert.assertEquals("str2", bean2.getTestRecursive2().getTestString().get());
		// Reference to object containing no dynamic values have to be kept
		Assert.assertTrue(bean1.getTestArray()==bean2.getTestArray());

	}
}
