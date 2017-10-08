package step.core.accessors;

import java.util.Map;

import org.bson.types.ObjectId;

public class AbstractDBObject {

	protected ObjectId _id;
	
	protected Map<String, String> attributes;
	
	public AbstractDBObject() {
		super();
		_id = new ObjectId();
	}

	public ObjectId getId() {
		return _id;
	}
	
	public void setId(ObjectId _id) {
		this._id = _id;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}
}
