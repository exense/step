package step.artefacts.reports;

import step.core.artefacts.reports.ReportNode;

public class TestStepReportNode extends ReportNode {

	protected String adapter;
	
	protected String input;
	
	protected String output;

	public TestStepReportNode() {
		super();
	}

	public String getAdapter() {
		return adapter;
	}

	public void setAdapter(String adapter) {
		this.adapter = adapter;
	}

	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}
}
