/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.artefacts.handlers;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonParsingException;

import org.bson.types.ObjectId;

import step.artefacts.CallFunction;
import step.artefacts.handlers.FunctionGroupHandler.FunctionGroupContext;
import step.artefacts.reports.CallFunctionReportNode;
import step.attachments.AttachmentMeta;
import step.common.managedoperations.OperationManager;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.execution.ExecutionContext;
import step.core.miscellaneous.ReportNodeAttachmentManager;
import step.core.miscellaneous.ReportNodeAttachmentManager.AttachmentQuotaException;
import step.core.plugins.ExecutionCallbacks;
import step.core.reports.Error;
import step.core.reports.ErrorType;
import step.core.variables.VariablesManager;
import step.datapool.DataSetHandle;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.execution.FunctionExecutionService;
import step.functions.handler.AbstractFunctionHandler;
import step.functions.io.FunctionInput;
import step.functions.io.Output;
import step.grid.Token;
import step.grid.TokenWrapper;
import step.grid.agent.tokenpool.TokenReservationSession;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;

public class CallFunctionHandler extends ArtefactHandler<CallFunction, CallFunctionReportNode> {

	protected FunctionExecutionService functionExecutionService;
	
	protected FunctionAccessor functionAccessor;
	
	protected ReportNodeAttachmentManager reportNodeAttachmentManager;
	protected DynamicJsonObjectResolver dynamicJsonObjectResolver;
	
	private static JsonProvider jprov = JsonProvider.provider();
			
	private SelectorHelper selectorHelper;
	
	private FunctionRouter functionRouter;
	
	protected FunctionLocator functionLocator;
	
	@Override
	public void init(ExecutionContext context) {
		super.init(context);
		functionExecutionService = context.get(FunctionExecutionService.class);
		functionAccessor = context.get(FunctionAccessor.class);
		functionRouter = context.get(FunctionRouter.class);
		reportNodeAttachmentManager = new ReportNodeAttachmentManager(context);
		dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler()));
		this.selectorHelper = new SelectorHelper(dynamicJsonObjectResolver);
		this.functionLocator = new FunctionLocator(functionAccessor, selectorHelper, context);
	}

	@Override
	protected void createReportSkeleton_(CallFunctionReportNode parentNode, CallFunction testArtefact) {}

	@Override
	protected void execute_(CallFunctionReportNode node, CallFunction testArtefact) throws Exception {
		String argumentStr = testArtefact.getArgument().get();
		node.setInput(argumentStr);
		
		Function function = getFunction(testArtefact);
		
		ExecutionCallbacks executionCallbacks = context.getExecutionCallbacks();
		executionCallbacks.beforeFunctionExecution(context, node, function);
		
		node.setFunctionId(function.getId().toString());
		node.setFunctionAttributes(function.getAttributes());

		FunctionInput<JsonObject> input = buildInput(argumentStr);
		node.setInput(input.getPayload().toString());
		
		validateInput(input, function);

		Output<JsonObject> output;
		if(!context.isSimulation()) {		
			Object o = context.getVariablesManager().getVariable(FunctionGroupHandler.FUNCTION_GROUP_CONTEXT_KEY);
			boolean releaseTokenAfterExecution = (o==null);
			
			CallFunctionTokenWrapperOwner tokenWrapperOwner = new CallFunctionTokenWrapperOwner(node.getId().toString(), context.getExecutionId(), context.getExecutionParameters().getDescription());
			TokenWrapper token = functionRouter.selectToken(testArtefact, function, (FunctionGroupContext)o, getBindings(), tokenWrapperOwner);
			try {
				Token gridToken = token.getToken();
				if(gridToken.isLocal()) {
					TokenReservationSession session = (TokenReservationSession) gridToken.getAttachedObject(TokenWrapper.TOKEN_RESERVATION_SESSION);
					session.put(AbstractFunctionHandler.EXECUTION_CONTEXT_KEY, context);
				}
				
				node.setAgentUrl(token.getAgent().getAgentUrl());
				node.setTokenId(token.getID());
				
				OperationManager.getInstance().enter("Keyword Call", new Object[]{function.getAttributes(), token.getToken(), token.getAgent()});
				
				try {
					output = functionExecutionService.callFunction(token.getID(), function, input, JsonObject.class);
				} finally {
					OperationManager.getInstance().exit();
				}
				
				Error error = output.getError();
				if(error!=null) {
					node.setError(error);
					node.setStatus(error.getType()==ErrorType.TECHNICAL?ReportNodeStatus.TECHNICAL_ERROR:ReportNodeStatus.FAILED);
				} else {
					node.setStatus(ReportNodeStatus.PASSED);
				}
	
				if(output.getPayload() != null) {
					context.getVariablesManager().putVariable(node, "output", output.getPayload());
					node.setOutput(output.getPayload().toString());
					node.setOutputObject(output.getPayload());
					ReportNode parentNode = context.getReportNodeCache().get(node.getParentID().toString());
					if(parentNode!=null) {
						context.getVariablesManager().putVariable(parentNode, "previous", output.getPayload());					
					}
				}
				
				if(output.getAttachments()!=null) {
					for(Attachment a:output.getAttachments()) {
						AttachmentMeta attachmentMeta;
						try {
							attachmentMeta = reportNodeAttachmentManager.createAttachment(AttachmentHelper.hexStringToByteArray(a.getHexContent()), a.getName());
							node.addAttachment(attachmentMeta);					
						} catch (AttachmentQuotaException e) {
							// attachment has been skipped. Nothing else to do here
						}
					}
				}
				if(output.getMeasures()!=null) {
					node.setMeasures(output.getMeasures());
				}
				
				String drainOutputValue = testArtefact.getResultMap().get();
				drainOutput(drainOutputValue, output);
			} finally {
				if(releaseTokenAfterExecution) {				
					functionExecutionService.returnTokenHandle(token.getID());
				}
	
				callChildrenArtefacts(node, testArtefact);
			}
		} else {
			output = new Output<>();
			output.setPayload(jprov.createObjectBuilder().build());
			node.setOutputObject(output.getPayload());
			node.setOutput(output.getPayload().toString());
			node.setStatus(ReportNodeStatus.PASSED);
		}
		
		executionCallbacks.afterFunctionExecution(context, node, function, output);
		
	}

	private void validateInput(FunctionInput<JsonObject> input, Function function) {
		if(context.getConfiguration().getPropertyAsBoolean("enforceschemas", false)){
			JsonSchemaValidator.validate(function.getSchema().toString(), input.getPayload().toString());
		}
	}

	private Function getFunction(CallFunction testArtefact) {
		return functionLocator.getFunction(testArtefact);
	}

	@SuppressWarnings("unchecked")
	private void drainOutput(String drainOutputValue, Output<JsonObject> output) {
		if(drainOutputValue!=null&&drainOutputValue.trim().length()>0) {
			JsonObject resultJson = output.getPayload();
			if(resultJson!=null) {
				Object var = context.getVariablesManager().getVariable(drainOutputValue);
				if(var instanceof Map) {
					Map<String, String> resultMap = jsonToMap(resultJson);
					((Map<String, String>)var).putAll(resultMap);
				} else if(var instanceof DataSetHandle) {
					DataSetHandle dataSetHandle = (DataSetHandle) var;
					Map<String, String> resultMap = jsonToMap(resultJson);
					for(String key:resultJson.keySet()) {
						JsonValue jsonValue = resultJson.get(key);
						if(jsonValue instanceof JsonArray) {
							JsonArray array = (JsonArray) jsonValue;
							array.forEach(value-> {
								if(value.getValueType().equals(ValueType.OBJECT)) {
									Map<String, String> rowAsMap = jsonToMap((JsonObject) value);
									dataSetHandle.addRow(rowAsMap);
								}
							});
						}
					}
					if(!resultMap.isEmpty()) {
						dataSetHandle.addRow(resultMap);						
					}
				} else {
					throw new RuntimeException("The variable '"+drainOutputValue+"' is neither a Map nor a DataSet handle");
				}					
			}
		}
	}

	private Map<String, String> jsonToMap(JsonObject jsonOutput) {
		Map<String, String> resultMap = new HashMap<>();
		for(String key:jsonOutput.keySet()) {
			JsonValue value = jsonOutput.get(key);
			if(value.getValueType() == ValueType.STRING) {
				resultMap.put(key, jsonOutput.getString(key));
			} else if (!value.getValueType().equals(ValueType.OBJECT)&&!value.getValueType().equals(ValueType.ARRAY)) {
				resultMap.put(key, jsonOutput.getString(key).toString());
			}
		}
		return resultMap;
	}

	protected void callChildrenArtefacts(CallFunctionReportNode node, CallFunction testArtefact) {
		if(testArtefact.getChildren()!=null&&testArtefact.getChildren().size()>0) {
			VariablesManager variableManager = context.getVariablesManager();
			variableManager.putVariable(node, "callReport", node);
			
//			node.getOutputObject().forEach((k,v)->{
//				variableManager.putVariable(node, k, v.toString());
//			});
			
			SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler(context);
			scheduler.execute_(node, testArtefact, true);				
		}
	}
	
	private FunctionInput<JsonObject> buildInput(String argumentStr) {
		JsonObject argument = parseAndResolveJson(argumentStr);
		
		Map<String, String> properties = new HashMap<>();
		context.getVariablesManager().getAllVariables().forEach((key,value)->properties.put(key, value!=null?value.toString():""));
		properties.put(AbstractFunctionHandler.PARENTREPORTID_KEY, context.getCurrentReportNode().getId().toString());
		
		FunctionInput<JsonObject> input = new FunctionInput<>();
		input.setPayload(argument);
		input.setProperties(properties);
		return input;
	}

	private JsonObject parseAndResolveJson(String functionStr) {
		JsonObject query;
		try {
			if(functionStr!=null&&functionStr.trim().length()>0) {
				query = jprov.createReader(new StringReader(functionStr)).readObject();
			} else {
				query = jprov.createObjectBuilder().build();
			}
		} catch(JsonParsingException e) {
			throw new RuntimeException("Error while parsing argument (input): "+e.getMessage());
		}
		return dynamicJsonObjectResolver.evaluate(query, getBindings());
	}


	@Override
	public CallFunctionReportNode createReportNode_(ReportNode parentNode, CallFunction testArtefact) {
		return new CallFunctionReportNode();
	}
}
