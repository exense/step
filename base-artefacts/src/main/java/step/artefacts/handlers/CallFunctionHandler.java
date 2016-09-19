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

import step.artefacts.CallFunction;
import step.artefacts.handlers.scheduler.SequentialArtefactScheduler;
import step.artefacts.reports.CallFunctionReportNode;
import step.attachments.AttachmentMeta;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.miscellaneous.ReportNodeAttachmentManager;
import step.core.miscellaneous.ReportNodeAttachmentManager.AttachmentQuotaException;
import step.functions.FunctionClient;
import step.functions.FunctionClient.FunctionToken;
import step.functions.Input;
import step.functions.Output;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.grid.tokenpool.Interest;
import step.plugins.adaptergrid.GridPlugin;

public class CallFunctionHandler extends ArtefactHandler<CallFunction, CallFunctionReportNode> {

	public static final String STEP_NODE_KEY = "currentStep";
	
	public CallFunctionHandler() {
		super();
	}

	@Override
	protected void createReportSkeleton_(CallFunctionReportNode parentNode, CallFunction testArtefact) {
		// TODO Auto-generated method stub
		
	}


	@SuppressWarnings("unchecked")
	@Override
	protected void execute_(CallFunctionReportNode node, CallFunction testArtefact) {
		String argumentStr = testArtefact.getArgument();
		node.setInput(argumentStr);
		
		Input input = buildInput(argumentStr);
		
		FunctionClient functionClient = (FunctionClient) ExecutionContext.getCurrentContext().getGlobalContext().get(GridPlugin.FUNCTIONCLIENT_KEY);
		
		boolean releaseTokenAfterExecution = false;
		FunctionToken functionToken;
		Object o = context.getVariablesManager().getVariable(FunctionGroupHandler.TOKEN_PARAM_KEY);
		if(o!=null && o instanceof FunctionToken) {
			functionToken = (FunctionToken) o;
		} else {
			String token = testArtefact.getToken();
			if(token!=null) {
				JsonObject selectionCriteriaJson = Json.createReader(new StringReader(token)).readObject();
				
				if(selectionCriteriaJson.getString("route").equals("local")) {
					functionToken = functionClient.getLocalFunctionToken();
				} else {
					Map<String, Interest> selectionCriteria = new HashMap<>();
					selectionCriteriaJson.keySet().stream().filter(e->!e.equals("route"))
						.forEach(key->selectionCriteria.put(key, new Interest(Pattern.compile(selectionCriteriaJson.getString(key)), true)));
					functionToken = functionClient.getFunctionToken(null, selectionCriteria);				
				}
				releaseTokenAfterExecution = true;
			} else {
				throw new RuntimeException("Token field hasn't been specified");
			}
		}
		
		node.setAdapter(functionToken.getToken()!=null?functionToken.getToken().getToken().getToken().getId():"local");
		
		Map<String, String> attributes = buildFunctionAttributesMap(testArtefact.getFunction());
		try {
			Output output = functionToken.call(attributes, input);
			node.setName(output.getFunction().getAttributes().get("name"));
			node.setFunctionId(output.getFunction().getId().toString());
			if(output.getError()!=null) {
				node.setError(output.getError());
				node.setStatus(ReportNodeStatus.TECHNICAL_ERROR);
			} else {
				node.setStatus(ReportNodeStatus.PASSED);
				if(output.getResult() != null) {
					context.getVariablesManager().putVariable(node, "output", output.getResult());
					node.setOutput(output.getResult().toString());
				}
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
				functionToken.release();
			}

			if(testArtefact.getChildrenIDs()!=null&&testArtefact.getChildrenIDs().size()>0) {
				context.getVariablesManager().putVariable(node, "callReport", node);
				SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler();
				scheduler.execute_(node, testArtefact);				
			}
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
		if(argumentStr!=null) {
			argument = Json.createReader(new StringReader(argumentStr)).readObject();
		} else {
			argument = Json.createObjectBuilder().build();
		}
		
		Map<String, String> properties = new HashMap<>();
		context.getVariablesManager().getAllVariables().forEach((key,value)->properties.put(key, value!=null?value.toString():""));

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
