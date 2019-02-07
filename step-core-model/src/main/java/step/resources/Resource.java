package step.resources;

import org.bson.types.ObjectId;

import step.core.accessors.AbstractOrganizableObject;

public class Resource extends AbstractOrganizableObject {

	protected ObjectId currentRevisionId;
	
	protected String resourceName;
	
	public ObjectId getCurrentRevisionId() {
		return currentRevisionId;
	}

	public void setCurrentRevisionId(ObjectId currentRevisionId) {
		this.currentRevisionId = currentRevisionId;
	}

	public String getResourceName() {
		return resourceName;
	}

	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}
}
