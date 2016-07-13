package step.grid.io;

import javax.json.JsonObject;

public class InputMessage {
	
	private String tokenId;

	private String function;
	
	private String handler;

	private JsonObject argument;

	public InputMessage() {
		super();
	}

	public String getTokenId() {
		return tokenId;
	}

	public void setTokenId(String tokenId) {
		this.tokenId = tokenId;
	}

	public String getFunction() {
		return function;
	}

	public void setFunction(String function) {
		this.function = function;
	}

	public String getHandler() {
		return handler;
	}

	public void setHandler(String handler) {
		this.handler = handler;
	}

	public JsonObject getArgument() {
		return argument;
	}

	public void setArguments(JsonObject argument) {
		this.argument = argument;
	}
	
	
}
