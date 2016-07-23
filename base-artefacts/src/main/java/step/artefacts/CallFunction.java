package step.artefacts;

import step.artefacts.handlers.CallFunctionHandler;
import step.artefacts.reports.TestStepReportNode;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.artefacts.DynamicAttribute;

@Artefact(handler = CallFunctionHandler.class, report = TestStepReportNode.class)
public class CallFunction extends AbstractArtefact {
	
	@DynamicAttribute
	String functionName;
	
	@DynamicAttribute
	String argument;
	
	@DynamicAttribute
	String resultMap;

	public String getFunctionName() {
		return functionName;
	}

	public void setFunctionName(String functionName) {
		this.functionName = functionName;
	}

	public String getArgument() {
		return argument;
	}

	public void setArgument(String argument) {
		this.argument = argument;
	}

	public String getResultMap() {
		return resultMap;
	}

	public void setResultMap(String resultMap) {
		this.resultMap = resultMap;
	}
}
