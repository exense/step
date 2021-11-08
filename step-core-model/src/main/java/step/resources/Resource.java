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

import step.core.accessors.AbstractOrganizableObject;
import step.core.objectenricher.EnricheableObject;

public class Resource extends AbstractOrganizableObject implements EnricheableObject {

	protected ObjectId currentRevisionId;
	
	protected String resourceType;
	
	protected String resourceName;
	
	protected boolean ephemeral;
	
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
}
