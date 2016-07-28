package step.artefacts.handlers;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;

import step.artefacts.CallFunction;
import step.artefacts.reports.TestStepReportNode;
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
import step.plugins.adaptergrid.GridPlugin;

public class CallFunctionHandler extends ArtefactHandler<CallFunction, TestStepReportNode> {

	public static final String STEP_NODE_KEY = "currentStep";
	
	public CallFunctionHandler() {
		super();
	}

	@Override
	protected void createReportSkeleton_(TestStepReportNode parentNode, CallFunction testArtefact) {
		// TODO Auto-generated method stub
		
	}


	@Override
	protected void execute_(TestStepReportNode node, CallFunction testArtefact) {
		String argumentStr = testArtefact.getArgument();
		JsonObject argument = Json.createReader(new StringReader(argumentStr)).readObject();
		
		String functionName = testArtefact.getFunctionName();
		
		Map<String, String> attributes = new HashMap<>();
		attributes.put("name", functionName);
		
		Input input = new Input();
		input.setArgument(argument);
		FunctionClient functionClient = (FunctionClient) ExecutionContext.getCurrentContext().getGlobalContext().get(GridPlugin.FUNCTIONCLIENT_KEY);
		
		boolean releaseTokenAfterExecution = false;
		FunctionToken functionToken;
		Object o = context.getVariablesManager().getVariable(FunctionGroupHandler.TOKEN_PARAM_KEY);
		if(o!=null && o instanceof FunctionToken) {
			functionToken = (FunctionToken) o;
		} else {
			functionToken = functionClient.getFunctionToken(null, null);
			releaseTokenAfterExecution = true;
		}
		
		try {
			Output output = functionToken.call(attributes, input);
			if(output.getError()!=null) {
				node.setError(output.getError());
				for(Attachment a:output.getAttachments()) {
					AttachmentMeta attachmentMeta;
					try {
						attachmentMeta = ReportNodeAttachmentManager.createAttachment(AttachmentHelper.hexStringToByteArray(a.getHexContent()), a.getName());
						node.addAttachment(attachmentMeta);					
					} catch (AttachmentQuotaException e) {
						logger.error("Error while converting attachment:" +a.getName(),e);
					}
				}
				node.setStatus(ReportNodeStatus.TECHNICAL_ERROR);
			} else {
				node.setStatus(ReportNodeStatus.PASSED);
				node.setOutput(output.getResult()!=null?output.getResult().toString():null);
			}
		} finally {
			if(releaseTokenAfterExecution) {				
				functionToken.release();
			}
		}
	}


	@Override
	public TestStepReportNode createReportNode_(ReportNode parentNode, CallFunction testArtefact) {
		return new TestStepReportNode();
	}
}
