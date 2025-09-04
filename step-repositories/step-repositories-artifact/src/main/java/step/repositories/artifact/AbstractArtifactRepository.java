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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.TestSet;
import step.automation.packages.AutomationPackageManager;
import step.automation.packages.execution.RepositoryWithAutomationPackageSupport;
import step.core.deployment.ControllerServiceException;
import step.core.execution.ExecutionContext;
import step.core.objectenricher.ObjectPredicate;
import step.core.repositories.ArtefactInfo;
import step.functions.accessor.FunctionAccessor;
import step.functions.type.FunctionTypeRegistry;
import step.resources.ResourceManager;

import java.io.File;
import java.util.Map;
import java.util.Set;

public abstract class AbstractArtifactRepository extends RepositoryWithAutomationPackageSupport {

	protected static final Logger logger = LoggerFactory.getLogger(MavenArtifactRepository.class);

	public AbstractArtifactRepository(Set<String> canonicalRepositoryParameters, AutomationPackageManager manager,
									  FunctionTypeRegistry functionTypeRegistry, FunctionAccessor functionAccessor,
									  ResourceManager resourceManager) {
		super(canonicalRepositoryParameters, manager, functionTypeRegistry, functionAccessor, resourceManager);
	}

	protected static String getMandatoryRepositoryParameter(Map<String, String> repositoryParameters, String paramKey) {
		String value = repositoryParameters.get(paramKey);
		if (value == null) {
			throw new ControllerServiceException("Missing required parameter " + paramKey);
		}
		return value;
	}

	@Override
	public ArtefactInfo getArtefactInfo(Map<String, String> repositoryParameters) {
		ArtefactInfo info = new ArtefactInfo();
		info.setName(resolveArtifactName(repositoryParameters));
		info.setType(TestSet.class.getSimpleName());
		return info;
	}

	protected abstract String resolveArtifactName(Map<String, String> repositoryParameters);

	public abstract File getArtifact(Map<String, String> repositoryParameters, ObjectPredicate objectPredicate);

	@Override
	public void exportExecution(ExecutionContext context, Map<String, String> repositoryParameters) {

	}

}
