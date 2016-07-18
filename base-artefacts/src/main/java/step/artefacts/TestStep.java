package step.artefacts;

import step.artefacts.handlers.TestStepHandler;
import step.artefacts.reports.TestStepReportNode;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;

@Artefact(name = "TestStep", handler = TestStepHandler.class, report = TestStepReportNode.class)
public class TestStep extends AbstractArtefact {
	
	private String input;
	
	private String expectedOutput;
	
	public String getInput() {
		return input;
	}

	public String getExpectedOutput() {
		return expectedOutput;
	}

	public void setInput(String input) {
		this.input = input;
	}

	public void setExpectedOutput(String expectedOutput) {
		this.expectedOutput = expectedOutput;
	}
}
