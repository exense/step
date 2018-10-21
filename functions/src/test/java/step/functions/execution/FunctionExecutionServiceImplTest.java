package step.functions.execution;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValue;
import step.core.dynamicbeans.DynamicValueResolver;
import step.expressions.ExpressionHandler;
import step.functions.Function;
import step.functions.Input;
import step.functions.Output;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.grid.TokenWrapper;
import step.grid.TokenWrapperOwner;
import step.grid.agent.handler.context.OutputMessageBuilder;
import step.grid.client.GridClient;
import step.grid.client.GridClientImpl.AgentCallTimeoutException;
import step.grid.client.GridClientImpl.AgentCommunicationException;
import step.grid.filemanager.FileManagerClient.FileVersionId;
import step.grid.io.AgentError;
import step.grid.io.AgentErrorCode;
import step.grid.io.OutputMessage;
import step.grid.tokenpool.Interest;

public class FunctionExecutionServiceImplTest {

	
	@Test
	public void testHappyPath() throws FunctionExecutionServiceException {
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(null, null, null, null);
		
		TokenWrapper token = f.getTokenHandle(new HashMap<>(), new HashMap<>(), true);
		token.setCurrentOwner(new TokenWrapperOwner() {});
		Input i = getDummyInput();
		Assert.assertFalse(beforeFunctionCallHasBeenCall.get());
		Output output = f.callFunction(token, new HashMap<>(), i);
		Assert.assertNotNull(output);
		Assert.assertTrue(beforeFunctionCallHasBeenCall.get());
		
		Assert.assertNull(output.getError());
		Assert.assertNotNull(output.getFunction());
		Assert.assertNotNull(token.getCurrentOwner());
		f.returnTokenHandle(token);
		Assert.assertNull(token.getCurrentOwner());
		
	}
	
	@Test
	public void testReserveError() {
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(null, null, new AgentCommunicationException("Reserve error"), null);
		
		FunctionExecutionServiceException e = null;
		try {
			f.getTokenHandle(new HashMap<>(), new HashMap<>(), true);
		} catch (FunctionExecutionServiceException e1) {
			e = e1;
		}
		Assert.assertNotNull(e);
		Assert.assertEquals("Communication error between the controller and the agent while reserving the agent token", e.getMessage());
	}
	
	@Test
	public void testReserveTimeoutError() {
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(null, null, new AgentCallTimeoutException(functionCallTimeout, "Reserve error", null), null);
		
		FunctionExecutionServiceException e = null;
		try {
			f.getTokenHandle(new HashMap<>(), new HashMap<>(), true);
		} catch (FunctionExecutionServiceException e1) {
			e = e1;
		}
		Assert.assertNotNull(e);
		Assert.assertEquals("Timeout after "+functionCallTimeout+"ms while reserving the agent token. You can increase the call timeout by setting 'grid.client.token.reserve.timeout.ms' in step.properties", e.getMessage());
	}
	
	@Test
	public void testReleaseError() throws FunctionExecutionServiceException {
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(null, null, null, new AgentCommunicationException("Release error"));
		
		FunctionExecutionServiceException e = null;
		TokenWrapper token = f.getTokenHandle(new HashMap<>(), new HashMap<>(), true);
		try {
			f.returnTokenHandle(token);
		} catch (FunctionExecutionServiceException e1) {
			e = e1;
		}
		Assert.assertNotNull(e);
		Assert.assertEquals("Communication error between the controller and the agent while releasing the agent token", e.getMessage());
	}
	
	@Test
	public void testReleaseTimeoutError() throws FunctionExecutionServiceException {
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(null, null, null, new AgentCallTimeoutException(functionCallTimeout, "Release error", null));
		
		FunctionExecutionServiceException e = null;
		TokenWrapper token = f.getTokenHandle(new HashMap<>(), new HashMap<>(), true);
		try {
			f.returnTokenHandle(token);
		} catch (FunctionExecutionServiceException e1) {
			e = e1;
		}
		Assert.assertNotNull(e);
		Assert.assertEquals("Timeout after "+functionCallTimeout+"ms while releasing the agent token. You can increase the call timeout by setting 'grid.client.token.release.timeout.ms' in step.properties", e.getMessage());
	}
	
	@Test
	public void testCallAgentCommunicationException() throws FunctionExecutionServiceException {
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(null, new AgentCommunicationException("Call error"), null, null);
		
		Output output = callFunctionWithDummyInput(f);
		Assert.assertNotNull(output);
		Assert.assertEquals("Communication error between the controller and the agent while calling the agent", output.getError());
	}
	
	@Test
	public void testCallTimeout() throws FunctionExecutionServiceException {
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(null, new AgentCallTimeoutException(functionCallTimeout, "Call timeout", null), null, null);
		
		Output output = callFunctionWithDummyInput(f);
		Assert.assertNotNull(output);
		Assert.assertEquals("Timeout after "+functionCallTimeout+ "ms while calling the agent. You can increase the call timeout in the configuration screen of the keyword", output.getError());
		
	}
	
	@Test
	public void testCallException() throws FunctionExecutionServiceException {
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(null, new Exception("Call exception", null), null, null);
		
		Output output = callFunctionWithDummyInput(f);
		Assert.assertNotNull(output);
		Assert.assertEquals("Unexpected error while calling keyword: java.lang.Exception Call exception", output.getError());
	}
	
	@Test
	public void testError() throws FunctionExecutionServiceException {
		OutputMessageBuilder outputMessageBuilder = new OutputMessageBuilder();
		OutputMessage outputMessage = outputMessageBuilder.setError("My error").build();
		
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(outputMessage, null, null, null);
		
		Output output = callFunctionWithDummyInput(f);
		Assert.assertNotNull(output);
		Assert.assertEquals("My error", output.getError());
	}
	
	@Test
	public void testCallAgentError() throws FunctionExecutionServiceException {
		OutputMessageBuilder outputMessageBuilder = new OutputMessageBuilder();
		OutputMessage outputMessage = outputMessageBuilder.build();
		outputMessage.setAgentError(new AgentError(AgentErrorCode.UNEXPECTED));
		
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(outputMessage, null, null, null);
		
		Output output = callFunctionWithDummyInput(f);
		Assert.assertNotNull(output);
		Assert.assertEquals("Unexepected error while executing the keyword on the agent", output.getError());
	}

	protected Output callFunctionWithDummyInput(FunctionExecutionService f)
			throws FunctionExecutionServiceException {
		TokenWrapper token = f.getTokenHandle(new HashMap<>(), new HashMap<>(), true);
		token.setCurrentOwner(new TokenWrapperOwner() {});
		Input i = getDummyInput();
		Output output = f.callFunction(token, new HashMap<>(), i);
		return output;
	}

	protected Input getDummyInput() {
		Input i = new Input();
		HashMap<String, String> inputProperties = new HashMap<>();
		inputProperties.put("inputProperty1", "inputProperty1");
		i.setProperties(inputProperties);
		i.setArgument(Json.createObjectBuilder().build());
		return i;
	}

	protected FunctionExecutionService getFunctionExecutionServiceForGridClientTest(OutputMessage outputMessage, Exception callException, AgentCommunicationException reserveTokenException, AgentCommunicationException returnTokenException) {
		GridClient gridClient = getGridClient(outputMessage, callException, reserveTokenException, returnTokenException);
		InMemoryFunctionAccessorImpl functionAccessor = getFunctionAccessor();
		FunctionTypeRegistry functionTypeRegistry = getFunctionTypeRegistry();
		DynamicBeanResolver dynamicBeanResolver = getDynamicBeanResolver();
		FunctionExecutionService f = new FunctionExecutionServiceImpl(gridClient, functionAccessor, functionTypeRegistry, dynamicBeanResolver);
		return f;
	}

	protected DynamicBeanResolver getDynamicBeanResolver() {
		return new DynamicBeanResolver(new DynamicValueResolver(new ExpressionHandler()));
	}

	AtomicBoolean beforeFunctionCallHasBeenCall = new AtomicBoolean(false);
	
	protected FunctionTypeRegistry getFunctionTypeRegistry() {
		return new FunctionTypeRegistry() {
			
			@Override
			public void registerFunctionType(AbstractFunctionType<? extends Function> functionType) {
			}
			
			@Override
			public AbstractFunctionType<Function> getFunctionTypeByFunction(Function function) {
				return dummyFunctionType();
			}
			
			@Override
			public AbstractFunctionType<Function> getFunctionType(String functionType) {
				return dummyFunctionType();
			}

			protected AbstractFunctionType<Function> dummyFunctionType() {
				return new AbstractFunctionType<Function>() {
					
					@Override
					public void beforeFunctionCall(Function function, Input input, Map<String, String> properties) {
						super.beforeFunctionCall(function, input, properties);
						beforeFunctionCallHasBeenCall.set(true);
					}

					@Override
					public Function newFunction() {
						return null;
					}
					
					@Override
					public Map<String, String> getHandlerProperties(Function function) {
						Map<String, String> handlerProperties = new HashMap<>();
						handlerProperties.put("handlerProperty1", "handlerProperty1");
						return handlerProperties;
					}
					
					@Override
					public String getHandlerChain(Function function) {
						return null;
					}
				};
			}
		};
	}

	int functionCallTimeout = 985;
	
	protected InMemoryFunctionAccessorImpl getFunctionAccessor() {
		InMemoryFunctionAccessorImpl f = new InMemoryFunctionAccessorImpl();
		Function function = new Function();
		function.setCallTimeout(new DynamicValue<Integer>(functionCallTimeout));
		function.setAttributes(new HashMap<>());
		f.save(function);
		return f;
	}

	protected GridClient getGridClient(OutputMessage outputMessage, Exception callException, AgentCommunicationException reserveTokenException, AgentCommunicationException returnTokenException) {
		return new GridClient() {
			
			@Override
			public String registerFile(File file) {
				throw new RuntimeException("Shouldn't be called");
			}
			
			@Override
			public File getRegisteredFile(String fileHandle) {
				return new File("testFile");
			}
			
			@Override
			public void returnTokenHandle(TokenWrapper tokenWrapper) throws AgentCommunicationException {
				if(returnTokenException != null) {
					throw returnTokenException;
				}
			}
			
			@Override
			public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests,
					boolean createSession) throws AgentCommunicationException {
				if(reserveTokenException != null) {
					throw reserveTokenException;
				} else {
					return new TokenWrapper();
				}
			}
			
			@Override
			public TokenWrapper getLocalTokenHandle() {
				return new TokenWrapper();
			}
			
			@Override
			public OutputMessage call(TokenWrapper tokenWrapper, String function, JsonObject argument, String handler,
					FileVersionId handlerPackage, Map<String, String> properties, int callTimeout) throws Exception {
				assert callTimeout == functionCallTimeout;
				assert properties.containsKey("inputProperty1");
				assert properties.containsKey("handlerProperty1");
				if(callException !=null) {
					throw callException;
				} else if(outputMessage !=null) {
					return outputMessage;					
				} else {
					return new OutputMessageBuilder().build();
				}
			}

			@Override
			public void close() {
			}
		};
	}

}
