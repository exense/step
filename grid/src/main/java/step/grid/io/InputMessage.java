package step.grid.io;

import java.util.Map;

import javax.json.JsonObject;

public class InputMessage {
	
	private String tokenId;

	private String function;
	
	private String handler;

	private JsonObject argument;
	
	private Map<String, String> properties;

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

	public void setArgument(JsonObject argument) {
		this.argument = argument;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}
	
	
}
