package step.attachments;

import org.bson.types.ObjectId;

public class AttachmentMeta {
	
	ObjectId _id;
	
	String name;

	public AttachmentMeta(ObjectId id) {
		super();
		_id = id;
	}
	
	public AttachmentMeta() {
		super();
		_id = new ObjectId();
	}
	
	public ObjectId getId() {
		return _id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
