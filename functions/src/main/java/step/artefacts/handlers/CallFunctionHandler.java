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
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonParsingException;

import step.artefacts.CallFunction;
import step.artefacts.reports.CallFunctionReportNode;
import step.attachments.AttachmentMeta;
import step.common.managedoperations.OperationManager;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.miscellaneous.ReportNodeAttachmentManager;
import step.core.miscellaneous.ReportNodeAttachmentManager.AttachmentQuotaException;
import step.datapool.DataSetHandle;
import step.functions.Function;
import step.functions.FunctionClient;
import step.functions.FunctionClient.FunctionTokenHandle;
import step.functions.Input;
import step.functions.Output;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.grid.tokenpool.Interest;
import step.plugins.adaptergrid.GridPlugin;

public class CallFunctionHandler extends ArtefactHandler<CallFunction, CallFunctionReportNode> {

	public static final String STEP_NODE_KEY = "currentStep";
	
	protected final FunctionClient functionClient;
	
	protected ReportNodeAttachmentManager reportNodeAttachmentManager;
	
	public CallFunctionHandler() {
		super();
		functionClient = (FunctionClient) context.getGlobalContext().get(GridPlugin.FUNCTIONCLIENT_KEY);
		reportNodeAttachmentManager = new ReportNodeAttachmentManager(context.getGlobalContext().getAttachmentManager());
	}

	@Override
	protected void createReportSkeleton_(CallFunctionReportNode parentNode, CallFunction testArtefact) {}


	@Override
	protected void execute_(CallFunctionReportNode node, CallFunction testArtefact) {
		String argumentStr = testArtefact.getArgument().get();
		node.setInput(argumentStr);
		
		Input input = buildInput(argumentStr);
				
		boolean releaseTokenAfterExecution = true;
		FunctionTokenHandle token;
		Object o = context.getVariablesManager().getVariable(FunctionGroupHandler.TOKEN_PARAM_KEY);
		if(o!=null && o instanceof FunctionTokenHandle) {
			token = (FunctionTokenHandle) o;
			releaseTokenAfterExecution = false;
		} else {
			token = selectToken(testArtefact, functionClient);
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
			
			OperationManager.getInstance().enter("Keyword Call", new Object[]{function.getAttributes(), token.getToken().getToken(), token.getAgentRef()});
			Output output;
			try {
				output = token.call(function.getId().toString(), input);
			} finally {
				OperationManager.getInstance().exit();
			}
			
			node.setName(function.getAttributes().get("name"));
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
		if(drainOutputValue!=null) {
			JsonObject resultJson = output.getResult();
			if(resultJson!=null) {
				Object var = context.getVariablesManager().getVariable(drainOutputValue);
				if(var instanceof Map) {
					Map<String, String> resultMap = jsonToMap(resultJson);
					((Map<String, String>)var).putAll(resultMap);
				} else if(var instanceof DataSetHandle) {
					DataSetHandle dataSetHandle = (DataSetHandle) var;
					Map<String, String> resultMap = jsonToMap(resultJson);
					if(resultJson.containsKey("@list")) {
						JsonArray array = resultJson.getJsonArray("@list");
						array.forEach(value-> {
							if(value.getValueType().equals(ValueType.OBJECT)) {
								Map<String, String> rowAsMap = jsonToMap((JsonObject) value);
								dataSetHandle.addRow(rowAsMap);
							}
						});
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
	
	private FunctionTokenHandle selectToken(CallFunction testArtefact, FunctionClient functionClient) {
		FunctionTokenHandle tokenHandle;
		String token = testArtefact.getToken().get();
		if(token!=null) {
			JsonObject selectionCriteriaJson = Json.createReader(new StringReader(token)).readObject();
			
			
			if(!testArtefact.getRemote().get()) {
				tokenHandle = functionClient.getLocalFunctionToken();
			} else {
				Map<String, Interest> selectionCriteria = new HashMap<>();
				selectionCriteriaJson.keySet().stream().forEach(key->selectionCriteria.put(key, new Interest(Pattern.compile(selectionCriteriaJson.getString(key)), true)));
				
				OperationManager.getInstance().enter("Token selection", selectionCriteria);
				try {
					tokenHandle = functionClient.getFunctionToken(null, selectionCriteria);
				} finally {
					OperationManager.getInstance().exit();					
				}
			}
		} else {
			throw new RuntimeException("Token field hasn't been specified");
		}
		return tokenHandle;
	}

	private void callChildrenArtefacts(CallFunctionReportNode node, CallFunction testArtefact) {
		if(testArtefact.getChildrenIDs()!=null&&testArtefact.getChildrenIDs().size()>0) {
			context.getVariablesManager().putVariable(node, "callReport", node);
			SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler();
			scheduler.execute_(node, testArtefact);				
		}
	}

	private Map<String, String> buildFunctionAttributesMap(String functionAttributesStr) {
		JsonObject attributesJson = Json.createReader(new StringReader(functionAttributesStr)).readObject();
		
		Map<String, String> attributes = new HashMap<>();
		attributesJson.forEach((key,value)->attributes.put(key, attributesJson.getString(key)));
		return attributes;
	}

	private Input buildInput(String argumentStr) {
		JsonObject argument;
		try {
			if(argumentStr!=null&&argumentStr.trim().length()>0) {
				argument = Json.createReader(new StringReader(argumentStr)).readObject();
			} else {
				argument = Json.createObjectBuilder().build();
			}
		} catch(JsonParsingException e) {
			throw new RuntimeException("Error while parsing argument (input): "+e.getMessage());
		}
		
		Map<String, String> properties = new HashMap<>();
		context.getVariablesManager().getAllVariables().forEach((key,value)->properties.put(key, value!=null?value.toString():""));
		properties.put("parentreportid", ExecutionContext.getCurrentReportNode().getId().toString());
		
		Input input = new Input();
		input.setArgument(argument);
		input.setProperties(properties);
		return input;
	}


	@Override
	public CallFunctionReportNode createReportNode_(ReportNode parentNode, CallFunction testArtefact) {
		return new CallFunctionReportNode();
	}
}
