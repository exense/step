package step.functions.services;

import java.util.Map;

import javax.json.JsonObject;

import step.functions.io.Input;
import step.grid.TokenWrapper;

public class CallFunctionInput {
	
	String functionId;
	Map<String, String> functionAttributes;
	TokenWrapper tokenHandle;
	Input<JsonObject> input;
	
	public CallFunctionInput() {
		super();
	}

	public String getFunctionId() {
		return functionId;
	}

	public void setFunctionId(String functionId) {
		this.functionId = functionId;
	}

	public TokenWrapper getTokenHandle() {
		return tokenHandle;
	}

	public void setTokenHandle(TokenWrapper tokenHandle) {
		this.tokenHandle = tokenHandle;
	}

	public Map<String, String> getFunctionAttributes() {
		return functionAttributes;
	}

	public void setFunctionAttributes(Map<String, String> functionAttributes) {
		this.functionAttributes = functionAttributes;
	}

	public Input<JsonObject> getInput() {
		return input;
	}

	public void setInput(Input<JsonObject> input) {
		this.input = input;
	}
	
}