/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.core.dynamicbeans;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import step.expressions.ExpressionHandler;

public class DynamicBeanResolverTest {

	private final DynamicBeanResolver resolver = new DynamicBeanResolver(new DynamicValueResolver(new ExpressionHandler()));
	
	@Test
	public void test1() {
		TestBean bean = new TestBean();
		resolver.evaluate(bean, null);
		Assert.assertEquals("test", bean.publicField.get());
		Assert.assertEquals("test", bean.publicFieldWithAnnotation.testString.get());
		Assert.assertEquals("test", bean.getTestString().get());
		Assert.assertEquals("test", bean.getTestRecursive().get().getTestString().get());
		Assert.assertEquals("test", bean.getTestRecursive2().getTestString().get());
	}
	
	@Test
	public void testClone() {
		TestBean bean = new TestBean();
		bean.testBoolean.setExpression("boolean1");
		bean.publicField.setExpression("string1");
		bean.publicFieldWithAnnotation.testString.setExpression("string1");
		bean.getTestRecursive2().getTestString().setExpression("string1");
		
		
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
		Assert.assertEquals("str1", bean1.publicField.get());
		Assert.assertEquals("str2", bean2.publicField.get());
		Assert.assertEquals("str1", bean1.publicFieldWithAnnotation.testString.get());
		Assert.assertEquals("str2", bean2.publicFieldWithAnnotation.testString.get());
		// Reference to object containing no dynamic values have to be kept
		Assert.assertSame(bean1.getTestArray(), bean2.getTestArray());

	}
}
