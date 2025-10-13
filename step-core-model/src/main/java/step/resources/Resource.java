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
package step.resources;

import org.bson.types.ObjectId;

import step.core.accessors.AbstractTrackedObject;
import step.core.objectenricher.EnricheableObject;

import java.util.Date;
import java.util.HashMap;

public class Resource extends AbstractTrackedObject implements EnricheableObject, Cloneable {

	public static final String TRACKING_FIELD = "tracking";

	protected ObjectId currentRevisionId;
	
	protected String resourceType;
	
	protected String resourceName;

	protected boolean directory;
	
	protected boolean ephemeral;

	protected String origin;

	protected Long originTimestamp;

	public Resource() {
		this(null);
	}

	public Resource(String actorUser){
		applyNewCreator(actorUser);
	}

	public ObjectId getCurrentRevisionId() {
		return currentRevisionId;
	}

	public void setCurrentRevisionId(ObjectId currentRevisionId) {
		this.currentRevisionId = currentRevisionId;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public String getResourceName() {
		return resourceName;
	}

	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

	public boolean isEphemeral() {
		return ephemeral;
	}

	public void setEphemeral(boolean ephemeral) {
		this.ephemeral = ephemeral;
	}

	public boolean isDirectory() {
		return directory;
	}

	public void setDirectory(boolean directory) {
		this.directory = directory;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public Long getOriginTimestamp() {
		return originTimestamp;
	}

	public void setOriginTimestamp(Long originTimestamp) {
		this.originTimestamp = originTimestamp;
	}

	public void applyNewCreator(String actorUser){
		Date now = new Date();
		setCreationDate(now);
		setCreationUser(actorUser);
		setLastModificationDate(now);
		setLastModificationUser(actorUser);
	}

	public Resource copy(String actorUser) {
		try {
			Resource copy = (Resource) this.clone();

			// create new instance of attributes and custom fields
			if (this.getAttributes() != null) {
				copy.setAttributes(new HashMap<>(this.getAttributes()));
			}
			if (this.getCustomFields() != null) {
				copy.setCustomFields(new HashMap<>(this.getCustomFields()));
			}

			// set new creation date and user
			copy.applyNewCreator(actorUser);
			return copy;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("Clone not supported", e);
		}
	}
}
