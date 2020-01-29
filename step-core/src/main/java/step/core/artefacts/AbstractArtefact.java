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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.MapDeserializer;
import step.core.accessors.MapSerializer;

@JsonTypeInfo(use=Id.CUSTOM,property="_class")
@JsonTypeIdResolver(ArtefactTypeIdResolver.class)
public abstract class AbstractArtefact extends AbstractOrganizableObject {
	
	protected String description;
		
	protected List<AbstractArtefact> children = new ArrayList<>();
	 
	@JsonSerialize(using = MapSerializer.class)
	@JsonDeserialize(using = MapDeserializer.class) 
	protected Map<String, Object> customAttributes;
	
	protected List<ObjectId> attachments;
	
	public AbstractArtefact() {
		super();
		_id = new ObjectId();
		
		Map<String, String> defaultAttributes = new HashMap<>();
		defaultAttributes.put("name", getDefaultArtefactName(this.getClass()));
		attributes = defaultAttributes;
	}
	
	private String getDefaultArtefactName(Class<? extends AbstractArtefact> artefactClass) {
		Artefact annotation = artefactClass.getAnnotation(Artefact.class);
		return annotation.name().length() > 0 ? annotation.name() : artefactClass.getSimpleName();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<AbstractArtefact> getChildren() {
		return children;
	}

	public void setChildren(List<AbstractArtefact> children) {
		this.children = children;
	}

	public boolean addChild(AbstractArtefact e) {
		return children.add(e);
	}

	public Map<String, Object> getCustomAttributes() {
		return customAttributes;
	}

	public void setCustomAttributes(Map<String, Object> customAttributes) {
		this.customAttributes = customAttributes;
	}

	public Object getCustomAttribute(String key) {
		if(customAttributes!=null) {
			return customAttributes.get(key);
		} else {
			return null;
		}
	}

	public synchronized void addCustomAttribute(String key, Object value) {
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

	@JsonIgnore
	public boolean isCreateSkeleton() {
		return false;
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
