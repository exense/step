package step.grid.io;

import javax.json.JsonObject;

public class OutputMessage {
	
	private JsonObject payload;

	public OutputMessage() {
		super();
	}

	public JsonObject getPayload() {
		return payload;
	}

	public void setPayload(JsonObject payload) {
		this.payload = payload;
	}
}
