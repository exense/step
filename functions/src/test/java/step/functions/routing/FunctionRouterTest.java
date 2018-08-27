package step.functions.routing;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;
import step.artefacts.CallFunction;
import step.commons.conf.Configuration;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionTestHelper;
import step.functions.Function;
import step.functions.FunctionClient;
import step.functions.type.AbstractFunctionType;
import step.grid.tokenpool.Interest;

public class FunctionRouterTest {

	protected ExecutionContext context;
	
	@Before
	public void setupContext() {
		context = ExecutionTestHelper.setupContext();
	}
	
	@Test
	public void test() {
		DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler()));

		FunctionClient client = new FunctionClient(context.getAttachmentManager(), new Configuration(), context.getDynamicBeanResolver(), null, null);
		FunctionRouter router = new FunctionRouter(client, client, dynamicJsonObjectResolver);
		
		CallFunction callFunction = new CallFunction();
		callFunction.getToken().setValue("{\"callFunction\":\"cf\"}");
		
		Function function = new Function();
		Map<String, String> map = new HashMap<>();
		map.put("function", "f");
		function.setTokenSelectionCriteria(map);
		client.registerFunctionType(new AbstractFunctionType<Function>() {

			@Override
			public String getHandlerChain(Function function) {
				return null;
			}

			@Override
			public Map<String, String> getHandlerProperties(Function function) {
				return null;
			}

			@Override
			public Function newFunction() {
				return function;
			}

			@Override
			public Map<String, Interest> getTokenSelectionCriteria(Function function) {
				Map<String, Interest> map = new HashMap<>();
				map.put("functionType", new Interest(Pattern.compile("ft"), true));
				return map;
			}
		});
		
		Map<String, Object> bindings = new HashMap<>();
		bindings.put("route_to_key", "val");
		Map<String, Interest> selectionCriteria = router.buildSelectionCriteriaMap(callFunction, function, null, bindings);
		Assert.assertEquals("val", selectionCriteria.get("key").getSelectionPattern().pattern());
		Assert.assertEquals("ft", selectionCriteria.get("functionType").getSelectionPattern().pattern());
		Assert.assertEquals("cf", selectionCriteria.get("callFunction").getSelectionPattern().pattern());
		Assert.assertEquals("f", selectionCriteria.get("function").getSelectionPattern().pattern());

	}
}
