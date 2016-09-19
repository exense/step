/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package step.core.artefacts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

@JsonTypeInfo(use=Id.CUSTOM,property="_class")
@JsonTypeIdResolver(ArtefactTypeIdResolver.class)
public abstract class AbstractArtefact {
	
	public ObjectId _id;
	
	protected Map<String, String> attributes;
		
	protected List<ObjectId> childrenIDs;
	
	protected Map<String, String> customAttributes;
	
	protected List<ObjectId> attachments;
	
	protected boolean createSkeleton = false;
	
	protected boolean root;
		
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
	
	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	public void addChild(ObjectId artefactID) {
		createChildrenIDListIfNeeded();
		childrenIDs.add(artefactID);
	}

	private void createChildrenIDListIfNeeded() {
		if(childrenIDs==null) {
			childrenIDs = new ArrayList<>();
		}
	}
	
	public void removeChild(ObjectId artefactID) {
		if(childrenIDs!=null) {
			childrenIDs.remove(artefactID);
		}
	}
	
	public int indexOf(ObjectId artefactID) {
		return childrenIDs!=null?childrenIDs.indexOf(artefactID):-1;
	}
	
	public void add(int pos, ObjectId artefactID) {
		createChildrenIDListIfNeeded();
		childrenIDs.add(pos, artefactID);
	}

	public List<ObjectId> getChildrenIDs() {
		return childrenIDs;
	}

	public void setChildrenIDs(List<ObjectId> childrenIDs) {
		this.childrenIDs = childrenIDs;
	}

	public boolean isRoot() {
		return root;
	}

	public void setRoot(boolean root) {
		this.root = root;
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
