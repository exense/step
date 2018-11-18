package step.core.accessors;

import org.bson.types.ObjectId;

public class AbstractIdentifiableObject {

	protected ObjectId _id;
	
	public AbstractIdentifiableObject() {
		super();
		_id = new ObjectId();
	}

	public ObjectId getId() {
		return _id;
	}
	
	public void setId(ObjectId _id) {
		this._id = _id;
	}
}
