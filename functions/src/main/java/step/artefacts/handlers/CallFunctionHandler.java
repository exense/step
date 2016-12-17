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
	
	public CallFunctionHandler() {
		super();
		functionClient = (FunctionClient) context.getGlobalContext().get(GridPlugin.FUNCTIONCLIENT_KEY);
	}

	@Override
	protected void createReportSkeleton_(CallFunctionReportNode parentNode, CallFunction testArtefact) {}


	@SuppressWarnings("unchecked")
	@Override
	protected void execute_(CallFunctionReportNode node, CallFunction testArtefact) {
		String argumentStr = testArtefact.getArgument();
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
			node.setAdapter(token.toString());
			token.setCurrentOwner(node);
			
			int callTimeoutDefault = context.getVariablesManager().getVariableAsInteger("keywords.calltimeout.default", 180000);
			token.setDefaultCallTimeout(callTimeoutDefault);
			
			Map<String, String> attributes = buildFunctionAttributesMap(testArtefact.getFunction());
			
			OperationManager.getInstance().enter("Keyword Call", new Object[]{attributes, token.getToken().getToken(), token.getAgentRef()});
			Output output;
			try {
				output = token.call(attributes, input);
			} finally {
				OperationManager.getInstance().exit();
			}
			
			Function function = output.getFunction();
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
						attachmentMeta = ReportNodeAttachmentManager.createAttachment(AttachmentHelper.hexStringToByteArray(a.getHexContent()), a.getName());
						node.addAttachment(attachmentMeta);					
					} catch (AttachmentQuotaException e) {
						logger.error("Error while converting attachment:" +a.getName(),e);
					}
				}
			}
			if(output.getMeasures()!=null) {
				node.setMeasures(output.getMeasures());
			}
			
			if(testArtefact.getResultMap()!=null) {
				Object var = context.getVariablesManager().getVariable(testArtefact.getResultMap());
				if(var instanceof Map) {
					JsonObject result = output.getResult();
					for(String key:result.keySet()) {
						JsonValue value = result.get(key);
						if(value.getValueType() == ValueType.STRING) {
							((Map<String, String>) var).put(key, result.getString(key));
						}
					}
				} else {
					throw new RuntimeException("The variable '"+testArtefact.getResultMap()+"' is not a Map");
				}
			}
		} finally {
			if(releaseTokenAfterExecution) {				
				token.release();
			}

			callChildrenArtefacts(node, testArtefact);
		}
	}

	private FunctionTokenHandle selectToken(CallFunction testArtefact, FunctionClient functionClient) {
		FunctionTokenHandle tokenHandle;
		String token = testArtefact.getToken();
		if(token!=null) {
			JsonObject selectionCriteriaJson = Json.createReader(new StringReader(token)).readObject();
			
			if(selectionCriteriaJson.getString("route").equals("local")) {
				tokenHandle = functionClient.getLocalFunctionToken();
			} else {
				Map<String, Interest> selectionCriteria = new HashMap<>();
				selectionCriteriaJson.keySet().stream().filter(e->!e.equals("route"))
					.forEach(key->selectionCriteria.put(key, new Interest(Pattern.compile(selectionCriteriaJson.getString(key)), true)));
				
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
