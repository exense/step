package step.artefacts.handlers;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.Test;

import step.artefacts.CallFunction;
import ch.exense.commons.core.model.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import ch.exense.commons.core.model.dynamicbeans.DynamicValue;
import step.core.objectenricher.ObjectPredicate;
import step.expressions.ExpressionHandler;
import step.functions.Function;
import step.functions.accessor.InMemoryFunctionAccessorImpl;

public class FunctionLocatorTest {

	private final InMemoryFunctionAccessorImpl functionAccessor = new InMemoryFunctionAccessorImpl();

	@Test
	public void test() {
		Function expectedFunction1 = newFunction("function1", "v1");
		Function expectedFunction2_noversion = newFunction("function2", null);
		Function expectedFunction2_v1 = newFunction("function2", "v1");
		Function expectedFunction2_v2 = newFunction("function2", "v2");
		
		FunctionLocator functionLocator = new FunctionLocator(functionAccessor, new SelectorHelper(
				new DynamicJsonObjectResolver(new DynamicJsonValueResolver(new ExpressionHandler()))));

		// by id
		CallFunction testArtefact = new CallFunction();
		testArtefact.setFunctionId(expectedFunction1.getId().toString());
		Function function = functionLocator.getFunction(testArtefact, predicate(), null);
		assertEquals(expectedFunction1, function);
		
		// by attributes
		CallFunction callFunction2 = new CallFunction();
		callFunction2.setFunction(new DynamicValue<String>("{\"name\":\"function1\"}"));
		Function function2 = functionLocator.getFunction(callFunction2, predicate(), null);
		assertEquals(expectedFunction1, function2);
		
		// by attributes with predicate
		Exception actual = null;
		try {
			functionLocator.getFunction(callFunction2, predicateFalse(), null);	
		} catch (Exception e) {
			actual = e;
		};
		assertNotNull(actual);
		
		// by attributes with dynamic expressions
		CallFunction callFunction4 = new CallFunction();
		callFunction4.setFunction(new DynamicValue<String>("{\"name\" : \"function2\", \"version\":{\"dynamic\":true, \"expression\": \"binding1\"}}"));
		Function function4;
		HashMap<String, Object> bindings = new HashMap<String, Object>();
		bindings.put("binding1", "v1");
		function4 = functionLocator.getFunction(callFunction4, predicate(), bindings);
		assertEquals(expectedFunction2_v1, function4);
		bindings.put("binding1", "v2");
		function4 = functionLocator.getFunction(callFunction4, predicate(), bindings);
		assertEquals(expectedFunction2_v2, function4);
		
		// using the special binding KEYWORD_ACTIVE_VERSIONS
		CallFunction callFunction5 = new CallFunction();
		callFunction5.setFunction(new DynamicValue<String>("{\"name\":\"function2\"}"));
		Function function5;
		HashMap<String, Object> bindings2 = new HashMap<String, Object>();
		bindings2.put(FunctionLocator.KEYWORD_ACTIVE_VERSIONS, "v1");
		function5 = functionLocator.getFunction(callFunction5, predicate(), bindings2);
		assertEquals(expectedFunction2_v1, function5);
		bindings2.put(FunctionLocator.KEYWORD_ACTIVE_VERSIONS, "v2");
		function5 = functionLocator.getFunction(callFunction5, predicate(), bindings2);
		assertEquals(expectedFunction2_v2, function5);
		// if no function is found for a specific version the first function without version is returned 
		bindings2.put(FunctionLocator.KEYWORD_ACTIVE_VERSIONS, "v3");
		function5 = functionLocator.getFunction(callFunction5, predicate(), bindings2);
		assertEquals(expectedFunction2_noversion, function5);
	}

	private Function newFunction(String name, String version) {
		Function function = new Function();
		function.addAttribute(AbstractOrganizableObject.NAME, name);
		if(version != null) {
			function.addAttribute(AbstractOrganizableObject.VERSION, version);
		}
		functionAccessor.save(function);
		return function;
	}

	private ObjectPredicate predicate() {
		return new ObjectPredicate() {
			@Override
			public boolean test(Object t) {
				return true;
			}
		};
	}
	
	private ObjectPredicate predicateFalse() {
		return new ObjectPredicate() {
			@Override
			public boolean test(Object t) {
				return false;
			}
		};
	}
}
