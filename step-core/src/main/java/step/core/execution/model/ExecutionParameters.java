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
package step.core.execution.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.serialization.EscapingDottedKeysMapDeserializer;
import step.core.accessors.serialization.EscapingDottedKeysMapSerializer;
import step.core.artefacts.ArtefactFilter;
import step.core.repositories.RepositoryObjectReference;

public class ExecutionParameters extends AbstractOrganizableObject {
	
	RepositoryObjectReference repositoryObject;
	
	List<RepositoryObjectReference> exports;
	
	String description;
	
	String userID;
		
	ArtefactFilter artefactFilter;
	
	ExecutionMode mode;
	
	@JsonSerialize(using = EscapingDottedKeysMapSerializer.class)
	@JsonDeserialize(using = EscapingDottedKeysMapDeserializer.class)
	Map<String, String> customParameters;
	
	boolean isolatedExecution = false;

	public ExecutionParameters() {
		super();
	}

	public ExecutionParameters(String userID, ArtefactFilter artefactFilter, ExecutionMode mode) {
		super();
		this.userID = userID;
		this.artefactFilter = artefactFilter;
		this.mode = mode;
	}

	public RepositoryObjectReference getRepositoryObject() {
		return repositoryObject;
	}

	public void setRepositoryObject(RepositoryObjectReference respositoryObject) {
		this.repositoryObject = respositoryObject;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getUserID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}

	public ArtefactFilter getArtefactFilter() {
		return artefactFilter;
	}

	public void setArtefactFilter(ArtefactFilter artefactFilter) {
		this.artefactFilter = artefactFilter;
	}

	public ExecutionMode getMode() {
		return mode;
	}

	public void setMode(ExecutionMode mode) {
		this.mode = mode;
	}
	
	public List<RepositoryObjectReference> getExports() {
		return exports;
	}

	public void setExports(List<RepositoryObjectReference> exports) {
		this.exports = exports;
	}

	public Map<String, String> getCustomParameters() {
		return customParameters;
	}

	public void setCustomParameters(Map<String, String> customParameters) {
		this.customParameters = customParameters;
	}

	public Boolean isIsolatedExecution() {
		return isolatedExecution;
	}

	public void setIsolatedExecution(Boolean isolatedExecution) {
		this.isolatedExecution = isolatedExecution;
	}

}
