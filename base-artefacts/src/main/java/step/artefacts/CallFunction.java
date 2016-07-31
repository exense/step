package step.artefacts;

import step.artefacts.handlers.CallFunctionHandler;
import step.artefacts.reports.TestStepReportNode;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.artefacts.DynamicAttribute;

@Artefact(handler = CallFunctionHandler.class, report = TestStepReportNode.class)
public class CallFunction extends AbstractArtefact {
	
	@DynamicAttribute
	String function;
	
	@DynamicAttribute
	String argument;
	
	@DynamicAttribute
	String token;

	public String getFunction() {
		return function;
	}

	public void setFunction(String function) {
		this.function = function;
	}

	public String getArgument() {
		return argument;
	}

	public void setArgument(String argument) {
		this.argument = argument;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}
