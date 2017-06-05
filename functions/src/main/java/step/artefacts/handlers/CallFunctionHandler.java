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
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonParsingException;

import step.artefacts.CallFunction;
import step.artefacts.reports.CallFunctionReportNode;
import step.attachments.AttachmentMeta;
import step.common.managedoperations.OperationManager;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.execution.ExecutionContext;
import step.core.miscellaneous.ReportNodeAttachmentManager;
import step.core.miscellaneous.ReportNodeAttachmentManager.AttachmentQuotaException;
import step.datapool.DataSetHandle;
import step.functions.Function;
import step.functions.FunctionClient;
import step.functions.FunctionClient.FunctionTokenHandle;
import step.functions.Input;
import step.functions.Output;
import step.functions.validation.JsonSchemaValidator;
import step.grid.Token;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.plugins.adaptergrid.GridPlugin;

public class CallFunctionHandler extends ArtefactHandler<CallFunction, CallFunctionReportNode> {

	public static final String STEP_NODE_KEY = "currentStep";
	
	protected FunctionClient functionClient;
	protected ReportNodeAttachmentManager reportNodeAttachmentManager;
	protected DynamicJsonObjectResolver dynamicJsonObjectResolver;
	
	private static JsonProvider jprov = JsonProvider.provider();
	
	private TokenSelectorHelper tokenSelectorHelper;
	
	@Override
	public void init(ExecutionContext context) {
		super.init(context);
		functionClient = (FunctionClient) context.getGlobalContext().get(GridPlugin.FUNCTIONCLIENT_KEY);
		reportNodeAttachmentManager = new ReportNodeAttachmentManager(context);
		dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getGlobalContext().getExpressionHandler()));
		this.tokenSelectorHelper = new TokenSelectorHelper(functionClient, dynamicJsonObjectResolver);
	}

	@Override
	protected void createReportSkeleton_(CallFunctionReportNode parentNode, CallFunction testArtefact) {}

	public static final String EXECUTION_CONTEXT_KEY = "$executionContext";
	
	@Override
	protected void execute_(CallFunctionReportNode node, CallFunction testArtefact) {
		String argumentStr = testArtefact.getArgument().get();
		node.setInput(argumentStr);
		
		Input input = buildInput(argumentStr);
		node.setInput(input.getArgument().toString());
		
		boolean releaseTokenAfterExecution = true;
		FunctionTokenHandle token;
		Object o = context.getVariablesManager().getVariable(FunctionGroupHandler.TOKEN_PARAM_KEY);
		if(o!=null && o instanceof FunctionTokenHandle) {
			token = (FunctionTokenHandle) o;
			releaseTokenAfterExecution = false;
		} else {
			token = tokenSelectorHelper.selectToken(testArtefact, functionClient, getBindings());
		}
		
		Token gridToken = token.getToken().getToken();
		if(gridToken.isLocal()) {
			gridToken.attachObject(EXECUTION_CONTEXT_KEY, context);
		}
				
		try {
			node.setAgentUrl(token.getAgentRef().getAgentUrl());
			node.setTokenId(token.getToken().getID());
			token.setCurrentOwner(node);
			
			int callTimeoutDefault = context.getVariablesManager().getVariableAsInteger("keywords.calltimeout.default", 180000);
			token.setDefaultCallTimeout(callTimeoutDefault);
			
			Function function;
			if(testArtefact.getFunctionId()!=null) {
				function = functionClient.getFunctionRepository().getFunctionById(testArtefact.getFunctionId());
			} else {
				Map<String, String> attributes = buildFunctionAttributesMap(testArtefact.getFunction());
				function = functionClient.getFunctionRepository().getFunctionByAttributes(attributes);
			}
			
			if(context.getGlobalContext().getConfiguration().getPropertyAsBoolean("enforceschemas", false)){
				JsonSchemaValidator.validate(function.getSchema().toString(), input.getArgument().toString());
			}
			
			OperationManager.getInstance().enter("Keyword Call", new Object[]{function.getAttributes(), token.getToken().getToken(), token.getAgentRef()});
			Output output;
			try {
				output = token.call(function.getId().toString(), input);
			} finally {
				OperationManager.getInstance().exit();
			}
			
			node.setFunctionId(function.getId().toString());
			
			String errorMsg = output.getError();
			if(errorMsg!=null) {
				node.setError(errorMsg, 0, true);
				node.setStatus(ReportNodeStatus.TECHNICAL_ERROR);
			} else {
				node.setStatus(ReportNodeStatus.PASSED);
			}

			if(output.getResult() != null) {
				context.getVariablesManager().putVariable(node, "output", output.getResult());
				node.setOutput(output.getResult().toString());
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
				token.release();
			}

			callChildrenArtefacts(node, testArtefact);
		}
	}

	@SuppressWarnings("unchecked")
	private void drainOutput(String drainOutputValue, Output output) {
		if(drainOutputValue!=null&&drainOutputValue.trim().length()>0) {
			JsonObject resultJson = output.getResult();
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

	private void callChildrenArtefacts(CallFunctionReportNode node, CallFunction testArtefact) {
		if(testArtefact.getChildrenIDs()!=null&&testArtefact.getChildrenIDs().size()>0) {
			context.getVariablesManager().putVariable(node, "callReport", node);
			SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler(context);
			scheduler.execute_(node, testArtefact);				
		}
	}

	private Map<String, String> buildFunctionAttributesMap(String functionAttributesStr) {
		JsonObject attributesJson = jprov.createReader(new StringReader(functionAttributesStr)).readObject();
		
		Map<String, String> attributes = new HashMap<>();
		attributesJson.forEach((key,value)->attributes.put(key, attributesJson.getString(key)));
		return attributes;
	}

	public static final String ARTEFACTID = "$artefactid";
	
	public static final String PARENTREPORTID = "$parentreportid";
	
	private Input buildInput(String argumentStr) {
		JsonObject argument;
		try {
			if(argumentStr!=null&&argumentStr.trim().length()>0) {
				argument = jprov.createReader(new StringReader(argumentStr)).readObject();
			} else {
				argument = jprov.createObjectBuilder().build();
			}
		} catch(JsonParsingException e) {
			throw new RuntimeException("Error while parsing argument (input): "+e.getMessage());
		}
		
		JsonObject argumentAfterResolving = dynamicJsonObjectResolver.evaluate(argument, getBindings());
		
		Map<String, String> properties = new HashMap<>();
		context.getVariablesManager().getAllVariables().forEach((key,value)->properties.put(key, value!=null?value.toString():""));
		properties.put(PARENTREPORTID, ExecutionContext.getCurrentReportNode().getId().toString());
		
		Input input = new Input();
		input.setArgument(argumentAfterResolving);
		input.setProperties(properties);
		return input;
	}


	@Override
	public CallFunctionReportNode createReportNode_(ReportNode parentNode, CallFunction testArtefact) {
		return new CallFunctionReportNode();
	}
}
