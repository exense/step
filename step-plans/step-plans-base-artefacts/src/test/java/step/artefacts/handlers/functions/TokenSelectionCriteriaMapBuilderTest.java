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
package step.artefacts.handlers.functions;

import org.junit.Test;
import step.artefacts.CallFunction;
import step.artefacts.handlers.FunctionGroupHandler.FunctionGroupContext;
import step.core.AbstractStepContext;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.expressions.ExpressionHandler;
import step.functions.Function;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.functions.type.FunctionTypeRegistryImpl;
import step.grid.tokenpool.Interest;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

public class TokenSelectionCriteriaMapBuilderTest {

	@Test
	public void testSelectionCriteriaMapBuilderPrecedence() {
		Function function = new Function();
		function.setTokenSelectionCriteria(Map.of("targetToken", "functionCriteria"));
		Function functionWithoutCriteria = new Function();
		functionWithoutCriteria.setTokenSelectionCriteria(Map.of());

		FunctionTypeRegistry functionTypeRegistry = buildFunctionTypeRegistry(Map.of("targetToken", new Interest(Pattern.compile("functionTypeCriteria"), true)));

		Map<String, Interest> functionGroupAttributes = Map.of("targetToken", new Interest(Pattern.compile("functionGroupCriteria"), true));
		FunctionGroupContext functionGroupContext = new FunctionGroupContext(null, functionGroupAttributes);

		CallFunction callFunction = new CallFunction();
		callFunction.getToken().setValue("{\"targetToken\":\"callFunctionArtefactCriteria\"}");

		Map<String, Object> bindings = Map.of("route_to_targetToken", "routeToCriteria");

		TokenSelectionCriteriaMapBuilder tokenSelectionCriteriaMapBuilder = getTokenSelectionCriteriaMapBuilder(functionTypeRegistry);

		// Prio 1: route_to binding
		// The binding route_to has precedence over all other way to define selection criteria
		Map<String, Interest> criteriaMap = tokenSelectionCriteriaMapBuilder.buildSelectionCriteriaMap(callFunction, function, functionGroupContext, bindings);
		assertEquals(Map.of("targetToken", new Interest(Pattern.compile("routeToCriteria"), true)), criteriaMap);

		// Prio 2: function criteria
		// The criteria defined for the function have precedence over all other way to define selection criteria after route_to
		criteriaMap = tokenSelectionCriteriaMapBuilder.buildSelectionCriteriaMap(callFunction, function, functionGroupContext, Map.of());
		assertEquals(Map.of("targetToken", new Interest(Pattern.compile("functionCriteria"), true)), criteriaMap);

		// Prio 3: function type criteria
		criteriaMap = tokenSelectionCriteriaMapBuilder.buildSelectionCriteriaMap(callFunction, functionWithoutCriteria, functionGroupContext, Map.of());
		assertEquals(Map.of("targetToken", new Interest(Pattern.compile("functionTypeCriteria"), true)), criteriaMap);

		// Prio 4: function group criteria
		FunctionTypeRegistry functionTypeRegistryWithoutCriteria = buildFunctionTypeRegistry(Map.of());
		tokenSelectionCriteriaMapBuilder = getTokenSelectionCriteriaMapBuilder(functionTypeRegistryWithoutCriteria);
		criteriaMap = tokenSelectionCriteriaMapBuilder.buildSelectionCriteriaMap(callFunction, functionWithoutCriteria, functionGroupContext, Map.of());
		assertEquals(Map.of("targetToken", new Interest(Pattern.compile("functionGroupCriteria"), true)), criteriaMap);

		// Prio 5: call function artefact keyword
		criteriaMap = tokenSelectionCriteriaMapBuilder.buildSelectionCriteriaMap(callFunction, functionWithoutCriteria, null, Map.of());
		assertEquals(Map.of("targetToken", new Interest(Pattern.compile("callFunctionArtefactCriteria"), true)), criteriaMap);
	}

	@Test
	public void testSelectionCriteriaMapBuilderAdditivity() {
		Function function = new Function();

		FunctionTypeRegistry functionTypeRegistry = buildFunctionTypeRegistry(Map.of());

		CallFunction callFunction = new CallFunction();
		callFunction.getToken().setValue("{\"criteriaAttribute1\":\"attribute1FromCallFunction\", \"criteriaAttribute2\":\"attribute2FromCallFunction\"}");

		Map<String, Object> bindings = new HashMap<>();
		bindings.put("route_to_criteriaAttribute1", "attribute1FromRouteTo");

		TokenSelectionCriteriaMapBuilder tokenSelectionCriteriaMapBuilder = getTokenSelectionCriteriaMapBuilder(functionTypeRegistry);
		Map<String, Interest> criteriaMap = tokenSelectionCriteriaMapBuilder.buildSelectionCriteriaMap(callFunction, function, null, bindings);
		assertEquals(Map.of("criteriaAttribute1", new Interest(Pattern.compile("attribute1FromRouteTo"), true), "criteriaAttribute2", new Interest(Pattern.compile("attribute2FromCallFunction"), true)), criteriaMap);

		Map<String, Interest> functionGroupAttributes = new HashMap<>();
		functionGroupAttributes.put("criteriaAttribute3", new Interest(Pattern.compile("attribute3FromFunctionGroup"), true));
		FunctionGroupContext functionGroupContext = new FunctionGroupContext(null, functionGroupAttributes);
		criteriaMap = tokenSelectionCriteriaMapBuilder.buildSelectionCriteriaMap(callFunction, function, functionGroupContext, bindings);
		assertEquals(Map.of("criteriaAttribute1", new Interest(Pattern.compile("attribute1FromRouteTo"), true),
				"criteriaAttribute2", new Interest(Pattern.compile("attribute2FromCallFunction"), true),
				"criteriaAttribute3", new Interest(Pattern.compile("attribute3FromFunctionGroup"), true)), criteriaMap);
	}

	private static TokenSelectionCriteriaMapBuilder getTokenSelectionCriteriaMapBuilder(FunctionTypeRegistry functionTypeRegistry) {
		DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(new ExpressionHandler()));
		return new TokenSelectionCriteriaMapBuilder(functionTypeRegistry, dynamicJsonObjectResolver);
	}

	private static FunctionTypeRegistry buildFunctionTypeRegistry(Map<String, Interest> criteria) {
		FunctionTypeRegistry functionTypeRegistry = new FunctionTypeRegistryImpl(null, null);
		functionTypeRegistry.registerFunctionType(new AbstractFunctionType<>() {

			@Override
			public String getHandlerChain(Function function) {
				return null;
			}

			@Override
			public Map<String, String> getHandlerProperties(Function function, AbstractStepContext executionContext) {
				return null;
			}

			@Override
			public Function newFunction() {
				return new Function();
			}

			@Override
			public Map<String, Interest> getTokenSelectionCriteria(Function function) {
				return criteria;
			}
		});
		return functionTypeRegistry;
	}
}
