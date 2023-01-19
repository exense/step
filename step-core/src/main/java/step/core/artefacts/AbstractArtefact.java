/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
 ******************************************************************************/
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
import step.core.dynamicbeans.DynamicValue;
import step.core.entities.EntityManager;
import step.core.entities.EntityReference;

@JsonTypeInfo(use=Id.CUSTOM,property="_class")
@JsonTypeIdResolver(ArtefactTypeIdResolver.class)
public abstract class AbstractArtefact extends AbstractOrganizableObject {

	protected DynamicValue<String> dynamicName;

	protected boolean useDynamicName;

	protected String description;
		
	protected List<AbstractArtefact> children = new ArrayList<>();
	 
	@JsonSerialize(using = MapSerializer.class)
	@JsonDeserialize(using = MapDeserializer.class) 
	protected Map<String, Object> customAttributes;
	
	protected List<ObjectId> attachments;
	
	protected boolean persistNode = true;
	
	private DynamicValue<Boolean> skipNode = new DynamicValue<>(false);
	private DynamicValue<Boolean> instrumentNode = new DynamicValue<>(false);
	private DynamicValue<Boolean> continueParentNodeExecutionOnError = new DynamicValue<>(false);
	
	public AbstractArtefact() {
		super();
		Map<String, String> defaultAttributes = new HashMap<>();
		defaultAttributes.put("name", getArtefactName(this.getClass()));
		attributes = defaultAttributes;
		persistNode = true;
		dynamicName = new DynamicValue<String>("");
		//dynamicName.setDynamic(true);
		dynamicName.setExpression("");
	}
	
	public static String getArtefactName(Class<? extends AbstractArtefact> artefactClass) {
		Artefact annotation = artefactClass.getAnnotation(Artefact.class);
		return annotation.name().length() > 0 ? annotation.name() : artefactClass.getSimpleName();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@EntityReference(type=EntityManager.recursive)
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
	
	/**
	 * Property artefacts are special artefacts that are directly attached to their
	 * parent artefact. Property artefacts are not subject to transclusion and
	 * remain attached to their parent. They are executed in 2 phases.
	 * <li>During the first phase the method ArtafactHandler.initProperties is
	 * called for each property artefacts before their parent artefact is
	 * executed.</li>
	 * <li>The second phase starts after execution of the parent artefact. During
	 * the second phase all the property artefact are executed
	 * (ArtafactHandler.execute_)</li>
	 * 
	 * @return true if this artefact is a property artefact
	 */
	@JsonIgnore
	public boolean isPropertyArefact() {
		return false;
	}

	public boolean isPersistNode() {
		return persistNode;
	}

	public void setPersistNode(boolean persistNode) {
		this.persistNode = persistNode;
	}

	public DynamicValue<Boolean> getSkipNode() {
		return skipNode;
	}

	public void setSkipNode(DynamicValue<Boolean> skipNode) {
		this.skipNode = skipNode;
	}

	public DynamicValue<String> getDynamicName() {
		return dynamicName;
	}

	public void setDynamicName(DynamicValue<String> dynamicName) {
		this.dynamicName = dynamicName;
	}

	public boolean isUseDynamicName() {
		return useDynamicName;
	}

	public void setUseDynamicName(boolean useDynamicName) {
		this.useDynamicName = useDynamicName;
	}

	public void setNameDynamically() {
		if (isUseDynamicName()) {
			String value = getDynamicName().get();
			if (value != null && !value.equals("")) {
				addAttribute(AbstractOrganizableObject.NAME, value);
			}
		}
	}

	public void deepCleanupAllCustomAttributes() {
		if (getCustomAttributes() != null) {
			getCustomAttributes().clear();
		}

		List<AbstractArtefact> children = getChildren();
		if (children != null) {
			for (AbstractArtefact child : children) {
				// repeat recursively for all children
				child.deepCleanupAllCustomAttributes();
			}
		}
	}

	public DynamicValue<Boolean> getInstrumentNode() {
		return instrumentNode;
	}

	public void setInstrumentNode(DynamicValue<Boolean> instrumentNode) {
		this.instrumentNode = instrumentNode;
	}

	public DynamicValue<Boolean> getContinueParentNodeExecutionOnError() {
		return continueParentNodeExecutionOnError;
	}

	public void setContinueParentNodeExecutionOnError(DynamicValue<Boolean> continueOnError) {
		this.continueParentNodeExecutionOnError = continueOnError;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
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
		if (getId() == null) {
			if (other.getId() != null)
				return false;
		} else if (!getId().equals(other.getId()))
			return false;
		return true;
	}
}
