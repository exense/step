package step.artefacts.handlers;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;

import step.artefacts.CallFunction;
import step.artefacts.reports.TestStepReportNode;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.functions.FunctionClient;
import step.functions.FunctionClient.FunctionToken;
import step.functions.Input;

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
		
		Input input = new Input();
		input.setArgument(argument);
		FunctionClient functionClient = (FunctionClient) ExecutionContext.getCurrentContext().getGlobalContext().get("");
		
		FunctionToken functionToken = functionClient.getFunctionToken(null, null);
		try {
			functionToken.call(functionName, input);			
		} finally {
			functionToken.release();
		}
	}


	@Override
	public TestStepReportNode createReportNode_(ReportNode parentNode, CallFunction testArtefact) {
		// TODO Auto-generated method stub
		return null;
	}
}
