package step.functions;

import java.util.List;

import javax.json.JsonObject;

import step.grid.io.Attachment;
import step.grid.io.Measure;

public class Output {
	
	private JsonObject result;
	
	private String error;
	
	private List<Attachment> attachments;
	
	private List<Measure> measures;
	
	private Function function;

	public JsonObject getResult() {
		return result;
	}

	public void setResult(JsonObject result) {
		this.result = result;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public List<Attachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}

	public Function getFunction() {
		return function;
	}

	public void setFunction(Function function) {
		this.function = function;
	}

	public List<Measure> getMeasures() {
		return measures;
	}

	public void setMeasures(List<Measure> measures) {
		this.measures = measures;
	}

}
