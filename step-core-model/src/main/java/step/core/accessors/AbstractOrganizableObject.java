package step.core.accessors;

import java.util.Map;

/**
 * This class extends {@link AbstractIdentifiableObject} and is used
 * as parent class for all the objects that should be organized or identified 
 * by custom attributes
 *
 */
public class AbstractOrganizableObject extends AbstractIdentifiableObject {
	
	protected Map<String, String> attributes;
	
	public static final String NAME = "name";
	
	public AbstractOrganizableObject() {
		super();
	}

	/**
	 * @return the attributes that identify this object's instance
	 */
	public Map<String, String> getAttributes() {
		return attributes;
	}

	/**
	 * Sets the attributes used to identify this object's instance
	 * 
	 * @param attributes the object's attributes as key-value pairs
	 */
	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}
}
