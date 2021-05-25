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
package step.artefacts.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Test;

import ch.exense.commons.io.FileHelper;
import junit.framework.Assert;
import step.artefacts.CallFunction;
import step.artefacts.reports.CallFunctionReportNode;
import step.attachments.AttachmentMeta;
import ch.exense.commons.core.model.accessors.AbstractOrganizableObject;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionContext;
import step.core.execution.model.ExecutionMode;
import step.core.reports.Error;
import step.core.reports.ErrorType;
import step.core.reports.Measure;
import step.datapool.DataSetHandle;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
import step.functions.io.FunctionInput;
import step.functions.io.Output;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.grid.AgentRef;
import step.grid.Token;
import step.grid.TokenWrapper;
import step.grid.TokenWrapperOwner;
import step.grid.agent.tokenpool.TokenReservationSession;
import step.grid.io.Attachment;
import step.grid.tokenpool.Interest;

public class CallFunctionHandlerTest extends AbstractArtefactHandlerTest {
	
	private static final ObjectId FUNCTION_ID_ERROR = new ObjectId();
	private static final ObjectId FUNCTION_ID_SUCCESS = new ObjectId();

	@After
	public void cleanup() {
		// delete the attachment folder created during the tests
		File attachmentFolder = new File("attachments");
		FileHelper.deleteFolder(attachmentFolder);
	}
	
	@Test
	public void test() {
		ExecutionContext executionContext = buildExecutionContext();
		
		Function function = newFunction(FUNCTION_ID_SUCCESS);
		((InMemoryFunctionAccessorImpl)executionContext.get(FunctionAccessor.class)).save(function);
		
		CallFunctionHandler handler = new CallFunctionHandler();
		handler.init(executionContext);
		
		CallFunction callFunction = new CallFunction();
		callFunction.setFunctionId(function.getId().toString());
		
		CallFunctionReportNode node = (CallFunctionReportNode) execute(callFunction);

		Assert.assertEquals(1, node.getAttachments().size());
		AttachmentMeta attachment = node.getAttachments().get(0);
		Assert.assertEquals("Attachment1", attachment.getName());
		
		Assert.assertEquals(1, node.getMeasures().size());
		
		Assert.assertEquals("{\"Output1\":\"Value1\"}", node.getOutput());
		Assert.assertEquals(ReportNodeStatus.PASSED, node.getStatus());
	}
	
	@Test
	public void testByAttributes() {
		ExecutionContext executionContext = buildExecutionContext();
		
		Function function = newFunction("MyFunction");
		((InMemoryFunctionAccessorImpl)executionContext.get(FunctionAccessor.class)).save(function);
		
		CallFunctionHandler handler = new CallFunctionHandler();
		handler.init(executionContext);
		
		CallFunction callFunction = new CallFunction();
		callFunction.setFunction(new DynamicValue<String>("{\"name\":\"MyFunction\"}"));
		
		CallFunctionReportNode node = (CallFunctionReportNode) execute(callFunction);

		Assert.assertEquals(1, node.getAttachments().size());
		AttachmentMeta attachment = node.getAttachments().get(0);
		Assert.assertEquals("Attachment1", attachment.getName());
		
		Assert.assertEquals(1, node.getMeasures().size());
		
		Assert.assertEquals("{\"Output1\":\"Value1\"}", node.getOutput());
		Assert.assertEquals(ReportNodeStatus.PASSED, node.getStatus());
	}
	
	@Test
	public void testDrainOutputToMap() {
		ExecutionContext executionContext = buildExecutionContext();
		
		Function function = newFunction(FUNCTION_ID_SUCCESS);
		((InMemoryFunctionAccessorImpl)executionContext.get(FunctionAccessor.class)).save(function);
		
		CallFunctionHandler handler = new CallFunctionHandler();
		handler.init(executionContext);
		
		Map<String, String> map = new HashMap<>();
		executionContext.getVariablesManager().putVariable(executionContext.getReport(),"map", map);
		
		CallFunction callFunction = new CallFunction();
		callFunction.setFunctionId(function.getId().toString());
		callFunction.setResultMap(new DynamicValue<String>("map"));
		
		CallFunctionReportNode node = (CallFunctionReportNode) execute(callFunction);

		Assert.assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		
		Assert.assertEquals("Value1", map.get("Output1"));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testDrainOutputToDataSetHandle() {
		ExecutionContext executionContext = buildExecutionContext();
		
		Function function = newFunction(FUNCTION_ID_SUCCESS);
		((InMemoryFunctionAccessorImpl)executionContext.get(FunctionAccessor.class)).save(function);
		
		CallFunctionHandler handler = new CallFunctionHandler();
		handler.init(executionContext);
		
		List<Object> addedRows = new ArrayList<>();
		DataSetHandle dataSetHandle = new DataSetHandle() {

			@Override
			public Object next() {
				return null;
			}

			@Override
			public void addRow(Object row) {
				addedRows.add(row);
			}
			
		};
		executionContext.getVariablesManager().putVariable(executionContext.getReport(),"dataSet", dataSetHandle);
		
		CallFunction callFunction = new CallFunction();
		callFunction.setFunctionId(function.getId().toString());
		callFunction.setResultMap(new DynamicValue<String>("dataSet"));
		
		CallFunctionReportNode node = (CallFunctionReportNode) execute(callFunction);

		Assert.assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		
		Assert.assertEquals("Value1", ((Map<String, String>)addedRows.get(0)).get("Output1"));
	}

	protected Function newFunction(ObjectId id) {
		Function function = new Function();
		function.setId(id);
		return function;
	}
	
	protected Function newFunction(String name) {
		Function function = new Function();
		
		Map<String, String> attributes = new HashMap<>();
		attributes.put(AbstractOrganizableObject.NAME, name);
		function.setAttributes(attributes);
		return function;
	}
	
	@Test
	public void testError() {
		ExecutionContext executionContext = buildExecutionContext();
		
		Function function = newFunction(FUNCTION_ID_ERROR);
		
		((InMemoryFunctionAccessorImpl)executionContext.get(FunctionAccessor.class)).save(function);
		
		CallFunctionHandler handler = new CallFunctionHandler();
		handler.init(executionContext);
		
		CallFunction callFunction = new CallFunction();
		callFunction.setFunctionId(function.getId().toString());
		
		CallFunctionReportNode node = (CallFunctionReportNode) execute(callFunction);
		
		Assert.assertEquals("My Error", node.getError().getMsg());
	}
	
	@Test
	public void testSimulation() {
		ExecutionContext executionContext = buildExecutionContext();
		executionContext.getExecutionParameters().setMode(ExecutionMode.SIMULATION);
		
		Function function = newFunction(FUNCTION_ID_SUCCESS);
		((InMemoryFunctionAccessorImpl)executionContext.get(FunctionAccessor.class)).save(function);
		
		CallFunctionHandler handler = new CallFunctionHandler();
		handler.init(executionContext);
		
		CallFunction callFunction = new CallFunction();
		callFunction.setFunctionId(function.getId().toString());
		
		CallFunctionReportNode node = (CallFunctionReportNode) execute(callFunction);
		
		Assert.assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		Assert.assertEquals("{}", node.getOutput());
		Assert.assertNull(node.getError());
	}

	protected ExecutionContext buildExecutionContext() {
		ExecutionContext executionContext = newExecutionContext();
		InMemoryFunctionAccessorImpl funcitonAccessor = new InMemoryFunctionAccessorImpl();
		
		FunctionTypeRegistry functionTypeRegistry = getFunctionTypeRepository();
		FunctionExecutionService functionExecutionService = getFunctionExecutionService();
		
		DefaultFunctionRouterImpl functionRouter = new DefaultFunctionRouterImpl(functionExecutionService, functionTypeRegistry, new DynamicJsonObjectResolver(new DynamicJsonValueResolver(executionContext.getExpressionHandler())));
		
		executionContext.put(FunctionAccessor.class, funcitonAccessor);
		executionContext.put(FunctionRouter.class, functionRouter);
		executionContext.put(FunctionExecutionService.class, functionExecutionService);
		
		context = executionContext;
		
		return executionContext;
	}

	protected FunctionExecutionService getFunctionExecutionService() {
		TokenWrapper token = getLocalToken();
		
		return new FunctionExecutionService() {
			
			@Override
			public void returnTokenHandle(String tokenId) throws FunctionExecutionServiceException {
				
			}
			
			@Override
			public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests,
					boolean createSession, TokenWrapperOwner tokenWrapperOwner) throws FunctionExecutionServiceException {
				return token;
			}
			
			@Override
			public TokenWrapper getLocalTokenHandle() {
				return null;
			}
			
			@SuppressWarnings("unchecked")
			@Override
			public <IN, OUT> Output<OUT> callFunction(String tokenId, Function function, FunctionInput<IN> input,	Class<OUT> outputClass) {
				return (Output<OUT>) newOutput(function.getId().toString());
			}
			
			protected Output<JsonObject> newOutput(String functionId) {
				if(functionId.equals(FUNCTION_ID_ERROR.toString())) {
					return newErrorOutput();
				} else {
					return newPassedOutput();
				}
			}
			
			protected Output<JsonObject> newPassedOutput() {
				Output<JsonObject> output = new Output<JsonObject>();
				
				List<Attachment> attachments = new ArrayList<>();
				Attachment attachment = new Attachment();
				attachment.setName("Attachment1");
				attachment.setHexContent("");
				attachments.add(attachment);
				output.setAttachments(attachments);
				
				List<Measure> measures = new ArrayList<>();
				measures.add(new Measure("Measure1", 1, 1, null));
				output.setMeasures(measures);
				
				output.setPayload(Json.createObjectBuilder().add("Output1", "Value1").build());
				
				return output;
			}
			
			protected Output<JsonObject> newErrorOutput() {
				Output<JsonObject> output = new Output<JsonObject>();
				output.setError(new Error(ErrorType.TECHNICAL, "keyword", "My Error", 0, true));
				return output;
			}
		};
	}

	protected TokenWrapper getLocalToken() {
		Token localToken = new Token();
		localToken.setAgentid("local");
		TokenWrapper token = new TokenWrapper();
		token.setToken(localToken);
		token.setAgent(new AgentRef());
		token.getToken().attachObject(TokenWrapper.TOKEN_RESERVATION_SESSION, new TokenReservationSession());
		return token;
	}

	protected FunctionTypeRegistry getFunctionTypeRepository() {
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
					public Function newFunction() {
						return null;
					}
					
					@Override
					public Map<String, String> getHandlerProperties(Function function) {
						return null;
					}
					
					@Override
					public String getHandlerChain(Function function) {
						return null;
					}
				};
			}
		};
	}
}

