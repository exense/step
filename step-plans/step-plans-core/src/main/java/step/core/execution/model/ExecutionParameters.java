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

import step.core.artefacts.ArtefactFilter;
import step.core.plans.Plan;
import step.core.repositories.RepositoryObjectReference;

public class ExecutionParameters extends CommonExecutionParameters {

	Plan plan;
	RepositoryObjectReference repositoryObject;

	boolean isolatedExecution = false;
	
	List<RepositoryObjectReference> exports;
	String description;

	public ExecutionParameters() {
		this((RepositoryObjectReference) null, null);
	}
	
	public ExecutionParameters(ExecutionMode mode) {
		this(mode, null, null, null, null, null, null, false, null);
	}

	public ExecutionParameters(RepositoryObjectReference repositoryObjectReference, String description, Map<String, String> customParameters) {
		this(ExecutionMode.RUN, null, repositoryObjectReference, customParameters, description, null, null, false, null);
	}
	public ExecutionParameters(RepositoryObjectReference repositoryObjectReference, Map<String, String> customParameters) {
		this(ExecutionMode.RUN, null, repositoryObjectReference, customParameters, null, null, null, false, null);
	}
	
	public ExecutionParameters(Plan plan, Map<String, String> customParameters) {
		this(ExecutionMode.RUN, plan, null, customParameters, defaultDescription(plan), null, null, false, null);
	}

	public ExecutionParameters(ExecutionMode mode, Plan plan, RepositoryObjectReference repositoryObject,
			Map<String, String> customParameters, String description, String userID, ArtefactFilter artefactFilter,
			boolean isolatedExecution, List<RepositoryObjectReference> exports) {
		super(customParameters, userID, artefactFilter, mode);
		this.plan = plan;
		this.repositoryObject = repositoryObject;
		this.description = description;
		this.isolatedExecution = isolatedExecution;
		this.exports = exports;
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

	public List<RepositoryObjectReference> getExports() {
		return exports;
	}

	public void setExports(List<RepositoryObjectReference> exports) {
		this.exports = exports;
	}

	public Boolean isIsolatedExecution() {
		return isolatedExecution;
	}

	public void setIsolatedExecution(Boolean isolatedExecution) {
		this.isolatedExecution = isolatedExecution;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
