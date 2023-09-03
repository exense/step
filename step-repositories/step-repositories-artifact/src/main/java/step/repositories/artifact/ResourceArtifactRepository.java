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
package step.repositories.artifact;

import step.core.plans.PlanAccessor;
import step.repositories.ArtifactRepositoryConstants;
import step.resources.ResourceManager;
import step.resources.ResourceRevisionFileHandle;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class ResourceArtifactRepository extends AbstractArtifactRepository {

	protected static final String PARAM_RESOURCE_ID = ArtifactRepositoryConstants.RESOURCE_PARAM_RESOURCE_ID;
	protected static final String PARAM_LIB_RESOURCE_ID = ArtifactRepositoryConstants.RESOURCE_PARAM_LIB_RESOURCE_ID;

	private final ResourceManager resourceManager;

	public ResourceArtifactRepository(PlanAccessor planAccessor, ResourceManager resourceManager) {
		super(Set.of(PARAM_RESOURCE_ID), planAccessor); // artifact_id = resource_id
		this.resourceManager = resourceManager;
	}

	@Override
	protected File getLibraries(Map<String, String> repositoryParameters) {
	    String resourceId = repositoryParameters.get(PARAM_LIB_RESOURCE_ID);
		if(resourceId != null){
			return getResourceFile(resourceId);
		} else {
			return null;
		}
	}

	@Override
	protected File getArtifact(Map<String, String> repositoryParameters) {
		String resourceId = getMandatoryRepositoryParameter(repositoryParameters, PARAM_RESOURCE_ID);
		return getResourceFile(resourceId);
	}

	@Override
	protected String resolveArtifactName(Map<String, String> repositoryParameters) {
		return AbstractArtifactRepository.getMandatoryRepositoryParameter(repositoryParameters, PARAM_RESOURCE_ID);
	}

	private File getResourceFile(String resourceId) {
		ResourceRevisionFileHandle resourceContent = resourceManager.getResourceFile(resourceId);
		if(resourceContent == null){
			throw new RuntimeException("Resource not found by id: " + resourceId);
		}
		return resourceContent.getResourceFile();
	}
}
