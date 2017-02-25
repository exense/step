package step.core.dynamicbeans;

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
}
