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
				return null;
			}

			@Override
			public Map<String, Interest> getTokenSelectionCriteria(Function function) {
				return criteria;
			}
		});
		return functionTypeRegistry;
	}

	// TODO remove if not rquired anymore
	/*
	@Test
	public void testMultipleTokensInContext() throws FunctionExecutionServiceException {
		CallFunction callFunction = new CallFunction();
		callFunction.getToken().setValue("{\"criteria1\":\"c1\"}");

		// FunctionGroupContext with a previously set token => The previously set token should be returned
		FunctionGroupContext functionGroupContext = new FunctionGroupContext(null);
		TokenWrapper token = router.selectToken(callFunction, function, functionGroupContext, new HashMap<>(), null);
		Assert.assertNotNull(token);
		Assert.assertEquals("c1",token.getAttributes().get("criteria1"));

		// Select a token with different criteria that do not match the attributes of the previously selected token
		// => a new token should be returned
		CallFunction callFunction2 = new CallFunction();
		callFunction2.getToken().setValue("{\"criteria2\":\"c2\"}");
		TokenWrapper token2 = router.selectToken(callFunction2, function, functionGroupContext, new HashMap<>(), null);
		Assert.assertNotNull(token2);
		Assert.assertEquals("c2",token2.getAttributes().get("criteria2"));

		// Select a token with the previous criteria => the previously selected token should be returned
		TokenWrapper token3 = router.selectToken(callFunction2, function, functionGroupContext, new HashMap<>(), null);
		Assert.assertTrue(token2 == token3);
	}

	@Test
	public void testLocalTokenRouting() throws FunctionExecutionServiceException {
		Function localFunction = new Function() {

			@Override
			public boolean requiresLocalExecution() {
				return true;
			}

		};

		CallFunction callFunction = new CallFunction();
		callFunction.getToken().setValue("{\"callFunction\":\"cf\"}");

		// No FunctionGroupContext => A new local token should be returned
		TokenWrapper token = router.selectToken(callFunction, localFunction, null, new HashMap<>(), null);
		Assert.assertEquals(LOCAL_TOKEN, token);

		// FunctionGroupContext without local token => A new local token should be returned and set to the FunctionGroupContext
		FunctionGroupContext functionGroupContext = new FunctionGroupContext(null);
		token = router.selectToken(callFunction, localFunction, functionGroupContext, new HashMap<>(), null);
		Assert.assertEquals(LOCAL_TOKEN, token);
		Assert.assertEquals(LOCAL_TOKEN, functionGroupContext.localToken);

		// FunctionGroupContext with a previously set token => The previously set token should be returned
		functionGroupContext = new FunctionGroupContext(null);
		functionGroupContext.setLocalToken(LOCAL_TOKEN_2);
		token = router.selectToken(callFunction, localFunction, functionGroupContext, new HashMap<>(), null);
		Assert.assertEquals(LOCAL_TOKEN_2, token);
	}

	private TokenWrapper LOCAL_TOKEN;
	private TokenWrapper LOCAL_TOKEN_2;

	private GridClient getDummyGridClient() {
		return new GridClient() {

			@Override
			public FileVersion registerFile(File file, boolean cleanable) throws FileManagerException {
				return new FileVersion(null, null, false);
			}

			@Override
			public FileVersion getRegisteredFile(FileVersionId fileVersionId) throws FileManagerException {
				return null;
			}

			@Override
			public void returnTokenHandle(String arg0) throws AgentCommunicationException {

			}

			@Override
			public void interruptTokenExecution(String s) throws GridClientException, AgentCommunicationException {

			}

			@Override
			public void markTokenAsFailing(String tokenId, String errorMessage, Exception e) {

			}

			@Override
			public void removeTokenError(String tokenId) {

			}

			@Override
			public void startTokenMaintenance(String tokenId) {

			}

			@Override
			public void stopTokenMaintenance(String tokenId) {

			}

			@Override
			public TokenWrapper getTokenHandle(Map<String, String> arg0, Map<String, Interest> arg1, boolean arg2)
					throws AgentCommunicationException {
				Map<String, String> attributes = new HashMap<>();
				arg1.entrySet().stream().forEach(e->attributes.put(e.getKey(), e.getValue().getSelectionPattern().toString()));
				return token(attributes);
			}

			@Override
			public TokenWrapper getLocalTokenHandle() {
				return LOCAL_TOKEN;
			}

			@Override
			public void close() {

			}

			@Override
			public List<AgentRef> getAgents() {
				return null;
			}

			@Override
			public List<TokenWrapper> getTokens() {
				return null;
			}

			@Override
			public OutputMessage call(String arg0, JsonNode arg2, String arg3, FileVersionId arg4,
					Map<String, String> arg5, int arg6) throws Exception {
				return null;
			}

			@Override
			public void unregisterFile(FileVersionId fileVersionId) {
				// TODO Auto-generated method stub

			}

			@Override
			public FileVersion registerFile(InputStream inputStream, String fileName, boolean isDirectory, boolean cleanable)
					throws FileManagerException {
				return new FileVersion(null, null, false);
			}

			@Override
			public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests,
					boolean createSession, TokenWrapperOwner tokenOwner) throws AgentCommunicationException {
				return getTokenHandle(attributes, interests, createSession);
			}
		};
	}
		@Test
	public void testRemoteTokenRouting() throws FunctionExecutionServiceException {
		CallFunction callFunction = new CallFunction();
		callFunction.getToken().setValue("{\"callFunction\":\"cf\"}");

		// No FunctionGroupContext => A remote token should be selected and returned
		Map<String, Interest> criteriaMap = router.buildSelectionCriteriaMap(callFunction, function, null, Map.of());
		assertEquals(Map.of("callFunction", new Interest(Pattern.compile("cf"), true)), criteriaMap);

		// FunctionGroupContext without local token => A new local token should be returned and set to the FunctionGroupContext
		FunctionGroupContext functionGroupContext = new FunctionGroupContext(functionExecutionService, null);
		assertEquals(Map.of("callFunction", new Interest(Pattern.compile("cf"), true)), criteriaMap);
	}

	*/

}
