package step.resources;

import org.bson.types.ObjectId;

import step.core.accessors.AbstractOrganizableObject;

public class Resource extends AbstractOrganizableObject {

	protected ObjectId currentRevisionId;
	
	protected String resourceType;
	
	protected String resourceName;
	
	protected boolean ephemeral;
	
	public ObjectId getCurrentRevisionId() {
		return currentRevisionId;
	}

	public void setCurrentRevisionId(ObjectId currentRevisionId) {
		this.currentRevisionId = currentRevisionId;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public String getResourceName() {
		return resourceName;
	}

	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

	public boolean isEphemeral() {
		return ephemeral;
	}

	public void setEphemeral(boolean ephemeral) {
		this.ephemeral = ephemeral;
	}
}
