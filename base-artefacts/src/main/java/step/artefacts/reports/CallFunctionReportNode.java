package step.artefacts.reports;

import java.util.List;

import step.core.artefacts.reports.ReportNode;
import step.grid.io.Measure;

public class CallFunctionReportNode extends ReportNode {

	protected String functionId;
	
	protected String adapter;
	
	protected String input;
	
	protected String output;
	
	private List<Measure> measures;

	public CallFunctionReportNode() {
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

	public String getFunctionId() {
		return functionId;
	}

	public void setFunctionId(String functionId) {
		this.functionId = functionId;
	}

	public List<Measure> getMeasures() {
		return measures;
	}

	public void setMeasures(List<Measure> measures) {
		this.measures = measures;
	}
}
