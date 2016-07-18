package step.expressions;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import step.commons.conf.Configuration;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionTestHelper;
import step.expressions.placeholder.PlaceHolderHandler;

public class ExpressionHandlerTest {

	@Test
	public void testEvaluateWithoutCache() {
		ExecutionTestHelper.setupContext();
		
		ExecutionContext c = ExecutionContext.getCurrentContext();
		c.getVariablesManager().putVariable(c.getReport(), "var1", "value1");
		
		Map<String, String> m = new HashMap<String, String>();
		m.put("outputParam1","outputValue1");
		
		PlaceHolderHandler p = new PlaceHolderHandler(c, m);
		ExpressionHandler h = new ExpressionHandler(p, null);
		
		String result = (String) h.evaluate("var1+outputParam1");
		Assert.assertEquals("value1outputValue1", result);
	}
	
	//TODO currently fails because of the dependency to ExcelFunctions in GroovyFunctions.groovy
	public void testEvaluateWithCache() {
		ExecutionTestHelper.setupContext();
		
		Configuration.getInstance().putProperty("tec.expressions.usecache", "true");
		
		ExecutionContext c = ExecutionContext.getCurrentContext();
		c.getVariablesManager().putVariable(c.getReport(), "var1", "value1");
		
		Map<String, String> m = new HashMap<String, String>();
		m.put("outputParam1","outputValue1");
		
		PlaceHolderHandler p = new PlaceHolderHandler(c, m);
		ExpressionHandler h = new ExpressionHandler(p, null);
		
		String result = (String) h.evaluate("var1+outputParam1");
		Assert.assertEquals("value1outputValue1", result);
	}
}
