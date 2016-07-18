package step.core.artefacts.reports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import step.attachments.AttachmentMeta;
import step.core.artefacts.AbstractArtefact;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use=Id.CLASS,property="_class")
public class ReportNode {
	
	public ObjectId _id;

	protected ObjectId parentID;
		
	protected String name;
	
	protected String executionID;

	protected ObjectId artefactID;
		
	protected long executionTime;
	
	protected Integer duration;
		
	protected List<AttachmentMeta> attachments;
		
	protected ReportNodeStatus status;
	
	protected String error;
	
	protected Map<String, String> customAttributes;
	
	protected AbstractArtefact artefactInstance;

	public ReportNode() {
		super();
		_id = new ObjectId();
	}

	public ObjectId getId() {
		return _id;
	}

	public ObjectId getParentID() {
		return parentID;
	}

	public void setParentID(ObjectId parentID) {
		this.parentID = parentID;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getExecutionID() {
		return executionID;
	}

	public void setExecutionID(String executionID) {
		this.executionID = executionID;
	}

	public ObjectId getArtefactID() {
		return artefactID;
	}

	public void setArtefactID(ObjectId artefactID) {
		this.artefactID = artefactID;
	}
	
	public long getExecutionTime() {
		return executionTime;
	}

	public void setExecutionTime(long executionTime) {
		this.executionTime = executionTime;
	}

	public Integer getDuration() {
		return duration;
	}

	public void setDuration(Integer duration) {
		this.duration = duration;
	}

	public List<AttachmentMeta> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<AttachmentMeta> attachments) {
		this.attachments = attachments;
	}

	public ReportNodeStatus getStatus() {
		return status;
	}

	public void setStatus(ReportNodeStatus status) {
		this.status = status;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public void addError(String error) {
		if(error!=null) {
			if(this.error!=null) {
				this.error = this.error+"\n"+error;
			} else {
				this.error = error;
			}
		}
	}
	
	public void addAttachment(AttachmentMeta attachment) {
		if(attachments==null) {
			attachments = new ArrayList<>();
		}
		attachments.add(attachment);
	}
	
	public String getCustomAttribute(String key) {
		if(customAttributes!=null) {
			return customAttributes.get(key);
		} else {
			return null;
		}
	}

	public synchronized void addCustomAttribute(String key, String value) {
		if(customAttributes==null) {
			customAttributes = new HashMap<>();
		}
		customAttributes.put(key, value);
	}

	public AbstractArtefact getArtefactInstance() {
		return artefactInstance;
	}

	public void setArtefactInstance(AbstractArtefact artefactInstance) {
		this.artefactInstance = artefactInstance;
	}

//	@Override
//	public String toString() {
//		return "ReportNode [name=" + name + ", id=" + getId() + "]";
//	}
}
