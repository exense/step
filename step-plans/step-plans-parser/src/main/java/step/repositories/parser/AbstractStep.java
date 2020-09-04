package step.repositories.parser;

import java.util.List;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use=Id.CLASS,property="_class")
public class AbstractStep {

	protected String name;
	
	protected List<ObjectId> attachments;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<ObjectId> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<ObjectId> attachments) {
		this.attachments = attachments;
	}

	@Override
	public String toString() {
		return name;
	}
}
