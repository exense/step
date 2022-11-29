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
package step.core.repositories;

import java.util.Map;

import step.core.execution.ExecutionContext;

public interface Repository {

	ArtefactInfo getArtefactInfo(Map<String, String> repositoryParameters) throws Exception;

	TestSetStatusOverview getTestSetStatusOverview(Map<String, String> repositoryParameters) throws Exception;

	ImportResult importArtefact(ExecutionContext context, Map<String, String> repositoryParameters) throws Exception;

	void exportExecution(ExecutionContext context, Map<String, String> repositoryParameters) throws Exception;

	/**
	 * Compares the canonical subset of the repository parameters.
	 * The canonical subset of the parameters corresponds to the minimal entries that uniquely identify
	 * the repository object in its repository
	 *
	 * @param repositoryParameters1
	 * @param repositoryParameters2
	 * @return true if the canonical subset of both repository parameters are the same
	 */
	boolean compareCanonicalRepositoryParameters(Map<String, String> repositoryParameters1, Map<String, String> repositoryParameters2);

}
