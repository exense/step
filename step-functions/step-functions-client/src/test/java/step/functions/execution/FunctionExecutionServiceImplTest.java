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
package step.functions.execution;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.dynamicbeans.DynamicBeanResolver;
import step.core.dynamicbeans.DynamicValue;
import step.core.dynamicbeans.DynamicValueResolver;
import step.expressions.ExpressionHandler;
import step.functions.Function;
import step.functions.handler.FunctionInputOutputObjectMapperFactory;
import step.functions.io.FunctionInput;
import step.functions.io.Input;
import step.functions.io.Output;
import step.functions.io.OutputBuilder;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionExecutionException;
import step.functions.type.FunctionTypeRegistry;
import step.grid.AgentRef;
import step.grid.Token;
import step.grid.TokenWrapper;
import step.grid.TokenWrapperOwner;
import step.grid.agent.handler.context.OutputMessageBuilder;
import step.grid.client.AbstractGridClientImpl.AgentCallTimeoutException;
import step.grid.client.AbstractGridClientImpl.AgentCommunicationException;
import step.grid.client.GridClient;
import step.grid.filemanager.FileManagerException;
import step.grid.filemanager.FileVersion;
import step.grid.filemanager.FileVersionId;
import step.grid.io.AgentError;
import step.grid.io.AgentErrorCode;
import step.grid.io.OutputMessage;
import step.grid.tokenpool.Interest;

public class FunctionExecutionServiceImplTest {

	
	@Test
	public void testHappyPath() throws FunctionExecutionServiceException {
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(null, null, null, null);
		
		TokenWrapper token = f.getTokenHandle(new HashMap<>(), new HashMap<>(), true, new TokenWrapperOwner() {});
		FunctionInput<JsonObject> i = getDummyInput();
		Assert.assertFalse(beforeFunctionCallHasBeenCall.get());
		Output<JsonObject> output = f.callFunction(token.getID(), getFunction(), i, JsonObject.class);
		Assert.assertNotNull(output);
		Assert.assertTrue(beforeFunctionCallHasBeenCall.get());
		
		Assert.assertNull(output.getError());
		f.returnTokenHandle(token.getID());
	}
	
	@Test
	public void testReserveError() {
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(null, null, new AgentCommunicationException("Reserve error"), null);
		
		FunctionExecutionServiceException e = null;
		try {
			f.getTokenHandle(new HashMap<>(), new HashMap<>(), true, null);
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
			f.getTokenHandle(new HashMap<>(), new HashMap<>(), true, null);
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
		TokenWrapper token = f.getTokenHandle(new HashMap<>(), new HashMap<>(), true, null);
		try {
			f.returnTokenHandle(token.getID());
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
		TokenWrapper token = f.getTokenHandle(new HashMap<>(), new HashMap<>(), true, null);
		try {
			f.returnTokenHandle(token.getID());
		} catch (FunctionExecutionServiceException e1) {
			e = e1;
		}
		Assert.assertNotNull(e);
		Assert.assertEquals("Timeout after "+functionCallTimeout+"ms while releasing the agent token. You can increase the call timeout by setting 'grid.client.token.release.timeout.ms' in step.properties", e.getMessage());
	}
	
	@Test
	public void testCallAgentCommunicationException() throws FunctionExecutionServiceException {
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(null, new AgentCommunicationException("Call error"), null, null);
		
		Output<JsonObject> output = callFunctionWithDummyInput(f);
		Assert.assertNotNull(output);
		Assert.assertEquals("Communication error between the controller and the agent while calling the agent", output.getError().getMsg());
	}
	
	@Test
	public void testCallTimeout() throws FunctionExecutionServiceException {
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(null, new AgentCallTimeoutException(functionCallTimeout, "Call timeout", null), null, null);
		
		Output<JsonObject> output = callFunctionWithDummyInput(f);
		Assert.assertNotNull(output);
		Assert.assertEquals("Timeout after "+functionCallTimeout+ "ms while calling the agent. You can increase the call timeout in the configuration screen of the keyword", output.getError().getMsg());
		
	}
	
	@Test
	public void testCallException() throws FunctionExecutionServiceException {
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(null, new Exception("Call exception", null), null, null);
		
		Output<JsonObject> output = callFunctionWithDummyInput(f);
		Assert.assertNotNull(output);
		Assert.assertEquals("Unexpected error while calling keyword: java.lang.Exception Call exception", output.getError().getMsg());
	}
	
	@Test
	public void testMeasures() throws FunctionExecutionServiceException {
		OutputBuilder outputBuilder = new OutputBuilder();
		
		outputBuilder.startMeasure("Measure1");
		outputBuilder.stopMeasure();
		
		Output<JsonObject> expectedOutput = outputBuilder.build();
		
		FunctionExecutionService f = getFunctionExecutionService(expectedOutput, null, null, null);
		
		Output<JsonObject> output = callFunctionWithDummyInput(f);
		Assert.assertNotNull(output);
		Assert.assertEquals(1, output.getMeasures().size());
	}
	
	@Test
	public void testError() throws FunctionExecutionServiceException {
		OutputBuilder outputBuilder = new OutputBuilder();
		Output<JsonObject> expectedOutput = outputBuilder.setError("My error").build();
		
		FunctionExecutionService f = getFunctionExecutionService(expectedOutput, null, null, null);
		
		Output<JsonObject> output = callFunctionWithDummyInput(f);
		Assert.assertNotNull(output);
		Assert.assertEquals("My error", output.getError().getMsg());
	}
	
	@Test
	public void testCallAgentError() throws FunctionExecutionServiceException {
		OutputMessageBuilder outputMessageBuilder = new OutputMessageBuilder();
		OutputMessage outputMessage = outputMessageBuilder.build();
		outputMessage.setAgentError(new AgentError(AgentErrorCode.UNEXPECTED));
		
		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(outputMessage, null, null, null);
		
		Output<JsonObject> output = callFunctionWithDummyInput(f);
		Assert.assertNotNull(output);
		Assert.assertEquals("Unexpected error while executing the keyword on the agent", output.getError().getMsg());
	}

	@Test
	public void testOutput() throws FunctionExecutionServiceException {
		OutputMessageBuilder outputMessageBuilder = new OutputMessageBuilder();
		outputMessageBuilder.setPayloadJson("{\"payload\":{\"stringValue\":\"string\"}}");
		OutputMessage outputMessage = outputMessageBuilder.build();

		FunctionExecutionService f = getFunctionExecutionServiceForGridClientTest(outputMessage, null, null, null);

		Output<JsonObject> output = callFunctionWithDummyInput(f);
		Assert.assertNotNull(output);
		Assert.assertEquals("string",output.getPayload().get("stringValue").toString());
	}

	protected Output<JsonObject> callFunctionWithDummyInput(FunctionExecutionService f)
			throws FunctionExecutionServiceException {
		TokenWrapper token = f.getTokenHandle(new HashMap<>(), new HashMap<>(), true, new TokenWrapperOwner() {});
		FunctionInput<JsonObject> i = getDummyInput();
		Output<JsonObject> output = f.callFunction(token.getID(), getFunction(), i, JsonObject.class);
		return output;
	}

	protected FunctionInput<JsonObject> getDummyInput() {
		FunctionInput<JsonObject> i = new FunctionInput<JsonObject>();
		HashMap<String, String> inputProperties = new HashMap<>();
		inputProperties.put("inputProperty1", "inputProperty1");
		i.setProperties(inputProperties);
		i.setPayload(Json.createObjectBuilder().build());
		return i;
	}

	protected FunctionExecutionService getFunctionExecutionServiceForGridClientTest(OutputMessage outputMessage, Exception callException, AgentCommunicationException reserveTokenException, AgentCommunicationException returnTokenException) {
		GridClient gridClient = getGridClient(outputMessage, callException, reserveTokenException, returnTokenException);
		FunctionTypeRegistry functionTypeRegistry = getFunctionTypeRegistry();
		DynamicBeanResolver dynamicBeanResolver = getDynamicBeanResolver();
		FunctionExecutionService f;
		try {
			f = new FunctionExecutionServiceImpl(gridClient, functionTypeRegistry, dynamicBeanResolver);
		} catch (FunctionExecutionServiceException e) {
			throw new RuntimeException(e);
		}
		return f;
	}
	
	protected FunctionExecutionService getFunctionExecutionService(Output<JsonObject> output, Exception callException, AgentCommunicationException reserveTokenException, AgentCommunicationException returnTokenException) {
		OutputMessageBuilder outputMessageBuilder = new OutputMessageBuilder();
		
		ObjectMapper mapper = FunctionInputOutputObjectMapperFactory.createObjectMapper();
		outputMessageBuilder.setPayload(mapper.valueToTree(output));
		
		return getFunctionExecutionServiceForGridClientTest(outputMessageBuilder.build(), callException, reserveTokenException, returnTokenException);
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
					public void beforeFunctionCall(Function function, Input<?> input, Map<String, String> properties) throws FunctionExecutionException {
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

	private Function getFunction() {
		Function function = new Function();
		function.setCallTimeout(new DynamicValue<Integer>(functionCallTimeout));
		function.setAttributes(new HashMap<>());
		return function;
	}

	protected GridClient getGridClient(OutputMessage outputMessage, Exception callException, AgentCommunicationException reserveTokenException, AgentCommunicationException returnTokenException) {
		return new GridClient() {
			
			@Override
			public FileVersion registerFile(File file) throws FileManagerException {
				return new FileVersion(null, null, false);
			}

			@Override
			public FileVersion getRegisteredFile(FileVersionId fileVersionId) throws FileManagerException {
				return new FileVersion(null, null, false);
			}
			
			@Override
			public void returnTokenHandle(String tokenWrapperId) throws AgentCommunicationException {
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
					return token();
				}
			}
			
			@Override
			public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests,
					boolean createSession, TokenWrapperOwner tokenOwner) throws AgentCommunicationException {
				TokenWrapper tokenHandle = getTokenHandle(attributes, interests, createSession);
				return tokenHandle;
			}
			
			@Override
			public TokenWrapper getLocalTokenHandle() {
				return token();
			}

			protected TokenWrapper token() {
				Token token = new Token();
				token.setId(UUID.randomUUID().toString());
				return new TokenWrapper(token, new AgentRef());
			}
			
			@Override
			public OutputMessage call(String tokenWrapperId, JsonNode argument, String handler,
					FileVersionId handlerPackage, Map<String, String> properties, int callTimeout) throws Exception {
				assert callTimeout == functionCallTimeout;
				assert !properties.containsKey("inputProperty1");
				assert !properties.containsKey("handlerProperty1");
				
				ObjectMapper mapper = FunctionInputOutputObjectMapperFactory.createObjectMapper();
				Input<?> input = mapper.treeToValue(argument, Input.class);
				assert input.getFunctionCallTimeout() == functionCallTimeout-100l;
				assert input.getProperties().containsKey("inputProperty1");
				assert input.getProperties().containsKey("handlerProperty1");
				
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

			@Override
			public void unregisterFile(FileVersionId fileVersionId) {
				
			}

			@Override
			public FileVersion registerFile(InputStream inputStream, String fileName, boolean isDirectory)
					throws FileManagerException {
				return new FileVersion(null, null, false);
			}
		};
	}

}
