package step.artefacts.handlers;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;

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


	@Override
	protected void execute_(CallFunctionReportNode node, CallFunction testArtefact) {
		String argumentStr = testArtefact.getArgument();
		
		node.setInput(argumentStr);
		
		JsonObject argument;
		if(argumentStr!=null) {
			argument = Json.createReader(new StringReader(argumentStr)).readObject();
		} else {
			argument = Json.createObjectBuilder().build();
		}
		
		String functionAttributesStr = testArtefact.getFunction();
		JsonObject attributesJson = Json.createReader(new StringReader(functionAttributesStr)).readObject();
		
		Map<String, String> attributes = new HashMap<>();
		attributesJson.forEach((key,value)->attributes.put(key, attributesJson.getString(key)));
		
		Input input = new Input();
		input.setArgument(argument);
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


	@Override
	public CallFunctionReportNode createReportNode_(ReportNode parentNode, CallFunction testArtefact) {
		return new CallFunctionReportNode();
	}
}
