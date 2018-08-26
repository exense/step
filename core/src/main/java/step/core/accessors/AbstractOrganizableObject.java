package step.core.accessors;

import java.util.Map;

public class AbstractOrganizableObject extends AbstractIdentifiableObject {
	
	protected Map<String, String> attributes;
	
	public AbstractOrganizableObject() {
		super();
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}
}
