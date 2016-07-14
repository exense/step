package step.grid.io;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;

public class OutputMessage {
	
	private JsonObject payload;
	
	private String error;
	
	private List<Attachment> attachments;

	public OutputMessage() {
		super();
	}

	public JsonObject getPayload() {
		return payload;
	}

	public void setPayload(JsonObject payload) {
		this.payload = payload;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public boolean addAttachment(Attachment arg0) {
		if(attachments==null) {
			attachments = new ArrayList<>();
		}
		return attachments.add(arg0);
	}

	public List<Attachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}
}
