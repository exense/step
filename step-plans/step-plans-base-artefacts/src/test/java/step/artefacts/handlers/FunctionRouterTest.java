package step.artefacts.handlers;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import junit.framework.Assert;
import step.artefacts.CallFunction;
import step.artefacts.handlers.FunctionGroupHandler.FunctionGroupContext;
import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.dynamicbeans.DynamicValueResolver;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionTestHelper;
import step.expressions.ExpressionHandler;
import step.functions.Function;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
import step.functions.execution.FunctionExecutionServiceImpl;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.functions.type.FunctionTypeRegistryImpl;
import step.grid.AgentRef;
import step.grid.Token;
import step.grid.TokenWrapper;
import step.grid.TokenWrapperOwner;
import step.grid.client.AbstractGridClientImpl.AgentCommunicationException;
import step.grid.client.GridClient;
import step.grid.filemanager.FileManagerException;
import step.grid.filemanager.FileVersion;
import step.grid.filemanager.FileVersionId;
import step.grid.io.OutputMessage;
import step.grid.tokenpool.Interest;

public class FunctionRouterTest {

	protected ExecutionContext context;
	
	protected DefaultFunctionRouterImpl router;
	
	protected Function function;
	
	@Before
	public void setupContext() throws Exception {
		context = ExecutionTestHelper.setupContext();
		
		function = new Function();
		Map<String, String> map = new HashMap<>();
		map.put("function", "f");
		function.setTokenSelectionCriteria(map);
		
		FunctionTypeRegistry functionTypeRegistry = new FunctionTypeRegistryImpl(null, null);
		functionTypeRegistry.registerFunctionType(new AbstractFunctionType<Function>() {

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
		
		FunctionExecutionService client = new FunctionExecutionServiceImpl(getDummyGridClient(), functionTypeRegistry, new DynamicBeanResolver(new DynamicValueResolver(new ExpressionHandler())));

		DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler()));
		router = new DefaultFunctionRouterImpl(client, functionTypeRegistry, dynamicJsonObjectResolver);
		
		LOCAL_TOKEN = token();
		LOCAL_TOKEN_2 = token();
	}

	protected TokenWrapper token() {
		return token(new HashMap<>());
	}
	
	protected TokenWrapper token(Map<String, String> attributes) {
		Token token = new Token();
		token.setAttributes(attributes);
		TokenWrapper tokenWrapper = new TokenWrapper(token, new AgentRef());
		return tokenWrapper;
	}
	
	@Test
	public void testSelectionCriteriaMapBuilder() throws FunctionExecutionServiceException {
		CallFunction callFunction = new CallFunction();
		callFunction.getToken().setValue("{\"callFunction\":\"cf\"}");
		
		Map<String, Object> bindings = new HashMap<>();
		bindings.put("route_to_key", "val");
		Map<String, Interest> selectionCriteria = router.buildSelectionCriteriaMap(callFunction, function, null, bindings);
		Assert.assertEquals("val", selectionCriteria.get("key").getSelectionPattern().pattern());
		Assert.assertEquals("ft", selectionCriteria.get("functionType").getSelectionPattern().pattern());
		Assert.assertEquals("cf", selectionCriteria.get("callFunction").getSelectionPattern().pattern());
		Assert.assertEquals("f", selectionCriteria.get("function").getSelectionPattern().pattern());
		
		Map<String, Interest> functionGroupAttributes = new HashMap<>();
		functionGroupAttributes.put("functionGroupContext", new Interest(Pattern.compile("fgc"), true));
		FunctionGroupContext functionGroupContext = new FunctionGroupContext(functionGroupAttributes);
		selectionCriteria = router.buildSelectionCriteriaMap(callFunction, function, functionGroupContext, bindings);
		Assert.assertEquals("fgc", selectionCriteria.get("functionGroupContext").getSelectionPattern().pattern());
	}
	
	@Test
	public void testRemoteTokenRouting() throws FunctionExecutionServiceException {
		CallFunction callFunction = new CallFunction();
		callFunction.getToken().setValue("{\"callFunction\":\"cf\"}");
		
		// No FunctionGroupContext => A remote token should be selected and returned
		TokenWrapper token = router.selectToken(callFunction, function, null, new HashMap<>(), null);
		Assert.assertNotNull(token);
		Assert.assertEquals("cf",token.getAttributes().get("callFunction"));
		
		// FunctionGroupContext without local token => A new local token should be returned and set to the FunctionGroupContext
		FunctionGroupContext functionGroupContext = new FunctionGroupContext(null);
		token = router.selectToken(callFunction, function, functionGroupContext, new HashMap<>(), null);
		Assert.assertNotNull(token);
		Assert.assertEquals("cf",token.getAttributes().get("callFunction"));
		Assert.assertEquals(token, functionGroupContext.tokens.get(0));
		
		// Token already in FunctionGroupContext => The same token should be returned
		TokenWrapper token2 = router.selectToken(callFunction, function, functionGroupContext, new HashMap<>(), null);
		Assert.assertTrue(token == token2);
	}
	
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
			public FileVersion registerFile(File file) throws FileManagerException {
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
			public OutputMessage call(String arg0, JsonNode arg2, String arg3, FileVersionId arg4,
					Map<String, String> arg5, int arg6) throws Exception {
				return null;
			}

			@Override
			public void unregisterFile(FileVersionId fileVersionId) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public FileVersion registerFile(InputStream inputStream, String fileName, boolean isDirectory)
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
}
