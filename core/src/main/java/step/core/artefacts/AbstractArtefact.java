package step.core.artefacts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use=Id.CLASS,property="_class")
public abstract class AbstractArtefact {
	
	public ObjectId _id;
	
	protected String name;
	
	protected List<ObjectId> childrenIDs;
	
	protected Map<String, String> customAttributes;
	
	protected List<ObjectId> attachments;
	
	protected boolean createSkeleton = false;
		
	public AbstractArtefact() {
		super();
		_id = new ObjectId();
	}

	public ObjectId getId() {
		return _id;
	}
	
	public void setId(ObjectId _id) {
		this._id = _id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public void addChild(ObjectId artefactID) {
		if(childrenIDs==null) {
			childrenIDs = new ArrayList<>();
		}
		childrenIDs.add(artefactID);
	}

	public List<ObjectId> getChildrenIDs() {
		return childrenIDs;
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
	
	public void addAttachment(ObjectId attachmentId) {
		if(attachments==null) {
			attachments = new ArrayList<>();
		}
		attachments.add(attachmentId);
	}
	
	public void setAttachments(List<ObjectId> attachments) {
		this.attachments = attachments;
	}

	public List<ObjectId> getAttachments() {
		return attachments;
	}

	public boolean isCreateSkeleton() {
		return createSkeleton;
	}

	public void setCreateSkeleton(boolean createSkeleton) {
		this.createSkeleton = createSkeleton;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_id == null) ? 0 : _id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractArtefact other = (AbstractArtefact) obj;
		if (_id == null) {
			if (other._id != null)
				return false;
		} else if (!_id.equals(other._id))
			return false;
		return true;
	}
}
