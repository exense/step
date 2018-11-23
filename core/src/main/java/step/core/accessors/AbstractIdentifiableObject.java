package step.core.accessors;

import org.bson.types.ObjectId;

/**
 * This class is the parent class of all objects that have to be identified
 * uniquely for persistence purposes for instance
 *
 */
public class AbstractIdentifiableObject {

	protected ObjectId _id;
	
	public AbstractIdentifiableObject() {
		super();
		_id = new ObjectId();
	}

	/**
	 * @return the unique ID of this object
	 */
	public ObjectId getId() {
		return _id;
	}
	
	/**
	 * @param _id the unique ID of this object
	 */
	public void setId(ObjectId _id) {
		this._id = _id;
	}
}
