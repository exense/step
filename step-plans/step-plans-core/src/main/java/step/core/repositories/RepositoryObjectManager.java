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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.execution.ExecutionContext;
import step.core.execution.model.ReportExport;
import step.core.execution.model.ReportExportStatus;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RepositoryObjectManager {

	private static final Logger logger = LoggerFactory.getLogger(RepositoryObjectManager.class);
	
	private Map<String, Repository> repositories = new ConcurrentHashMap<>();
	
	public RepositoryObjectManager() {
		super();
	}
	
	public void registerRepository(String id, Repository repository) {
		repositories.put(id, repository);
	}

	public ImportResult importPlan(ExecutionContext context, RepositoryObjectReference artefact) throws Exception  {
		String respositoryId = artefact.getRepositoryID();
		Repository repository = getRepository(respositoryId);
		return repository.importArtefact(context, artefact.getRepositoryParameters());
	}

	private Repository getRepository(String respositoryId) {
		Repository repository = repositories.get(respositoryId);
		if(repository==null) {
			throw new RuntimeException("Unknown repository '"+respositoryId+"'");
		}
		return repository;
	}
	
	public ReportExport exportTestExecutionReport(ExecutionContext context, RepositoryObjectReference report) {	
		ReportExport export = new ReportExport();
		if(report != null) {
			String respositoryId = report.getRepositoryID();
			try {
				Repository repository = getRepository(respositoryId);
				repository.exportExecution(context, report.getRepositoryParameters());	
				export.setStatus(ReportExportStatus.SUCCESSFUL);
			} catch (Exception e) {
				export.setStatus(ReportExportStatus.FAILED);
				export.setError(e.getMessage() + ". See application logs for more details.");
				logger.error("Error while exporting test " + context.getExecutionId() + " to " + respositoryId,e);
			}			
		}
		return export;
	}
	
	public ArtefactInfo getArtefactInfo(RepositoryObjectReference ref) throws Exception {
		String respositoryId = ref.getRepositoryID();
		try {
			Repository repository = getRepository(respositoryId);
			return repository.getArtefactInfo(ref.getRepositoryParameters());
		} catch (Exception e) {
			logger.error("Error while getting artefact infos for " + ref,e);
			throw e;
		}
	}
	
	public TestSetStatusOverview getReport(RepositoryObjectReference report) throws Exception {
		String respositoryId = report.getRepositoryID();
		Repository repository = getRepository(respositoryId);
		return repository.getTestSetStatusOverview(report.getRepositoryParameters());
	}

	public boolean compareRepositoryObjectReference(RepositoryObjectReference ref1, RepositoryObjectReference ref2) {
		String repositoryId1 = ref1.getRepositoryID();
		String repositoryId2 = ref2.getRepositoryID();
		if(Objects.equals(repositoryId1, repositoryId2)) {
			Repository repository = getRepository(repositoryId1);
			return repository.compareCanonicalRepositoryParameters(ref1.getRepositoryParameters(), ref2.getRepositoryParameters());
		} else {
			return false;
		}
	}

	public Set<String> getAllRepositoriesCanonicalParameters() {
		return repositories.values().stream().map(Repository::getCanonicalRepositoryParameters)
				.flatMap(Set::stream).collect(Collectors.toSet());
	}
}
