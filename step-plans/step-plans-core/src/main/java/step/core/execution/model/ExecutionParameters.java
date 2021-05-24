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
package step.core.execution.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import ch.exense.commons.core.model.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactFilter;
import ch.exense.commons.core.accessors.serialization.EscapingDottedKeysMapDeserializer;
import ch.exense.commons.core.accessors.serialization.EscapingDottedKeysMapSerializer;
import step.core.plans.Plan;
import step.core.repositories.RepositoryObjectReference;

public class ExecutionParameters extends AbstractOrganizableObject {
	
	private static final String DEFAULT_DESCRIPTION = "Unnamed";
	private static final String DEFAULT_USERID = "dummy";
	
	ExecutionMode mode;

	Plan plan;
	RepositoryObjectReference repositoryObject;

	@JsonSerialize(using = EscapingDottedKeysMapSerializer.class)
	@JsonDeserialize(using = EscapingDottedKeysMapDeserializer.class)
	Map<String, String> customParameters;
	
	String description;
	String userID;
		
	ArtefactFilter artefactFilter;
	
	/**
	 * @deprecated this field is deprecated and isn't used anymore
	 */
	boolean isolatedExecution = false;
	
	List<RepositoryObjectReference> exports;

	public ExecutionParameters() {
		this((RepositoryObjectReference) null, null);
	}
	
	public ExecutionParameters(ExecutionMode mode) {
		this(mode, null, null, null, null, DEFAULT_USERID, null, false, null);
	}

	public ExecutionParameters(RepositoryObjectReference repositoryObjectReference, Map<String, String> customParameters) {
		this(ExecutionMode.RUN, null, repositoryObjectReference, customParameters, null, DEFAULT_USERID, null, false, null);
	}
	
	public ExecutionParameters(Plan plan, Map<String, String> customParameters) {
		this(ExecutionMode.RUN, plan, null, customParameters, defaultDescription(plan), DEFAULT_USERID, null, false, null);
	}

	public ExecutionParameters(ExecutionMode mode, Plan plan, RepositoryObjectReference repositoryObject,
			Map<String, String> customParameters, String description, String userID, ArtefactFilter artefactFilter,
			boolean isolatedExecution, List<RepositoryObjectReference> exports) {
		super();
		this.mode = mode;
		this.plan = plan;
		this.repositoryObject = repositoryObject;
		this.customParameters = customParameters;
		this.description = description;
		this.userID = userID;
		this.artefactFilter = artefactFilter;
		this.isolatedExecution = isolatedExecution;
		this.exports = exports;
	}
	
	public static String defaultDescription(Plan plan) {
		String description;
		Map<String, String> attributes = plan.getAttributes();
		if(attributes != null && attributes.containsKey(AbstractArtefact.NAME)) {
			description = attributes.get(AbstractArtefact.NAME);
		} else {
			AbstractArtefact root = plan.getRoot();
			if(root != null) {
				attributes = root.getAttributes();
				if(attributes != null && attributes.containsKey(AbstractArtefact.NAME)) {
					description = attributes.get(AbstractArtefact.NAME);
				} else {
					description = DEFAULT_DESCRIPTION;
				}
			} else {
				description = DEFAULT_DESCRIPTION;
			}
		}
		return description;
	}

	public Plan getPlan() {
		return plan;
	}

	public void setPlan(Plan plan) {
		this.plan = plan;
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

	/**
	 * @deprecated This field is deprecated and isn't used anymore
	 * @return
	 */
	public Boolean isIsolatedExecution() {
		return isolatedExecution;
	}

	/**
	 * @deprecated This field is deprecated and isn't used anymore
	 * @param isolatedExecution
	 */
	public void setIsolatedExecution(Boolean isolatedExecution) {
		this.isolatedExecution = isolatedExecution;
	}

}
