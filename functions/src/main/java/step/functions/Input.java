package step.functions;

import java.util.Map;

import javax.json.JsonObject;

public class Input {

	JsonObject argument;
	
	Map<String, String> properties;

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
