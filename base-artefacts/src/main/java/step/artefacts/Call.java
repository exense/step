package step.artefacts;

import step.artefacts.handlers.CallHandler;
import step.artefacts.reports.TestStepReportNode;
import step.core.artefacts.Artefact;
import step.core.artefacts.DynamicAttribute;
import step.core.artefacts.AbstractArtefact;

@Artefact(handler = CallHandler.class, report = TestStepReportNode.class)
public class Call extends AbstractArtefact {
	
	@DynamicAttribute
	String procedure;
	
	@DynamicAttribute
	String arguments;
	
	@DynamicAttribute
	String resultMap;

	public String getProcedure() {
		return procedure;
	}

	public void setProcedure(String procedure) {
		this.procedure = procedure;
	}

	public String getArguments() {
		return arguments;
	}

	public void setArguments(String arguments) {
		this.arguments = arguments;
	}

	public String getResultMap() {
		return resultMap;
	}

	public void setResultMap(String resultMap) {
		this.resultMap = resultMap;
	}
}
