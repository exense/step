package step.artefacts.handlers;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import junit.framework.Assert;
import step.artefacts.CallFunction;
import step.artefacts.handlers.FunctionGroupHandler.FunctionGroupContext;
import step.artefacts.handlers.FunctionRouter;
import step.attachments.AttachmentManager;
import step.attachments.FileResolver;
import step.commons.conf.Configuration;
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
import step.grid.TokenWrapper;
import step.grid.client.GridClient;
import step.grid.client.GridClientImpl.AgentCommunicationException;
import step.grid.filemanager.FileManagerException;
import step.grid.filemanager.FileVersion;
import step.grid.filemanager.FileVersionId;
import step.grid.io.OutputMessage;
import step.grid.tokenpool.Interest;

public class FunctionRouterTest {

	protected ExecutionContext context;
	
	protected FunctionRouter router;
	
	protected Function function;
	
	@Before
	public void setupContext() throws Exception {
		context = ExecutionTestHelper.setupContext();
		
		function = new Function();
		Map<String, String> map = new HashMap<>();
		map.put("function", "f");
		function.setTokenSelectionCriteria(map);
		
		FunctionTypeRegistry functionTypeRegistry = new FunctionTypeRegistryImpl(new FileResolver(new AttachmentManager(new Configuration())), null);
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
		router = new FunctionRouter(client, functionTypeRegistry, dynamicJsonObjectResolver);
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
		TokenWrapper token = router.selectToken(callFunction, function, null, new HashMap<>());
		Assert.assertEquals(REMOTE_TOKEN, token);
		
		// FunctionGroupContext without local token => A new local token should be returned and set to the FunctionGroupContext
		FunctionGroupContext functionGroupContext = new FunctionGroupContext(null);
		token = router.selectToken(callFunction, function, functionGroupContext, new HashMap<>());
		Assert.assertEquals(REMOTE_TOKEN, token);
		Assert.assertEquals(REMOTE_TOKEN, functionGroupContext.token);
		
		// FunctionGroupContext with a previously set token => The previously set token should be returned
		functionGroupContext = new FunctionGroupContext(null);
		functionGroupContext.setToken(FUNCTION_GROUP_TOKEN);
		token = router.selectToken(callFunction, function, functionGroupContext, new HashMap<>());
		Assert.assertEquals(FUNCTION_GROUP_TOKEN, token);
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
		TokenWrapper token = router.selectToken(callFunction, localFunction, null, new HashMap<>());
		Assert.assertEquals(LOCAL_TOKEN, token);

		// FunctionGroupContext without local token => A new local token should be returned and set to the FunctionGroupContext
		FunctionGroupContext functionGroupContext = new FunctionGroupContext(null);
		token = router.selectToken(callFunction, localFunction, functionGroupContext, new HashMap<>());
		Assert.assertEquals(LOCAL_TOKEN, token);
		Assert.assertEquals(LOCAL_TOKEN, functionGroupContext.localToken);
		
		// FunctionGroupContext with a previously set token => The previously set token should be returned
		functionGroupContext = new FunctionGroupContext(null);
		functionGroupContext.setLocalToken(FUNCTION_GROUP_TOKEN);
		token = router.selectToken(callFunction, localFunction, functionGroupContext, new HashMap<>());
		Assert.assertEquals(FUNCTION_GROUP_TOKEN, token);
	}

	private static final TokenWrapper REMOTE_TOKEN = new TokenWrapper();
	private static final TokenWrapper LOCAL_TOKEN = new TokenWrapper();
	private static final TokenWrapper FUNCTION_GROUP_TOKEN = new TokenWrapper();
	
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
			public void returnTokenHandle(TokenWrapper arg0) throws AgentCommunicationException {
				
			}
			
			@Override
			public TokenWrapper getTokenHandle(Map<String, String> arg0, Map<String, Interest> arg1, boolean arg2)
					throws AgentCommunicationException {
				return REMOTE_TOKEN;
			}
			
			@Override
			public TokenWrapper getLocalTokenHandle() {
				return LOCAL_TOKEN;
			}
			
			@Override
			public void close() {
				
			}
			
			@Override
			public OutputMessage call(TokenWrapper arg0, JsonNode arg2, String arg3, FileVersionId arg4,
					Map<String, String> arg5, int arg6) throws Exception {
				return null;
			}
		};
	}
}
