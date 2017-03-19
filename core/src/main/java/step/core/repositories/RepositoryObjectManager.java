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
package step.core.repositories;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactAccessor;
import step.core.execution.model.ReportExport;
import step.core.execution.model.ReportExportStatus;

public class RepositoryObjectManager {

	private static final Logger logger = LoggerFactory.getLogger(RepositoryObjectManager.class);
	
	public static final String CLIENT_KEY = "RepositoryObjectManager_Client";
	
	private Map<String, Repository> repositories = new ConcurrentHashMap<>();
	
	private ArtefactAccessor artefactAccessor;
	
	public RepositoryObjectManager(ArtefactAccessor artefactAccessor) {
		super();
		this.artefactAccessor = artefactAccessor;
	}
	
	public void registerRepository(String id, Repository repository) {
		repositories.put(id, repository);
	}

	public String importArtefact(RepositoryObjectReference artefact) throws Exception  {
		String respositoryId = artefact.getRepositoryID();
		Repository repository = getRepository(respositoryId);
		return repository.importArtefact(artefact.getRepositoryParameters());
	}

	private Repository getRepository(String respositoryId) {
		Repository repository = repositories.get(respositoryId);
		if(repository==null) {
			throw new RuntimeException("Unknown repository '"+respositoryId+"'");
		}
		return repository;
	}
	
	public ReportExport exportTestExecutionReport(RepositoryObjectReference report, String executionID) {	
		ReportExport export = new ReportExport();

		String respositoryId = report.getRepositoryID();
		Repository repository = getRepository(respositoryId);
		
		try {
			repository.exportExecution(report.getRepositoryParameters(), executionID);	
			export.setStatus(ReportExportStatus.SUCCESSFUL);
		} catch (Exception e) {
			export.setStatus(ReportExportStatus.FAILED);
			export.setError(e.getMessage() + ". See application logs for more details.");
			logger.error("Error while exporting test " + executionID + " to " + respositoryId,e);
		}
		return export;
	}
	
	private static final String LOCAL = "local";
	private static final String ARTEFACT_ID = "artefactid";
	
	public ArtefactInfo getArtefactInfo(RepositoryObjectReference ref) throws Exception {
		if(ref.getRepositoryID().equals(LOCAL)) {
			String artefactid = ref.getRepositoryParameters().get(ARTEFACT_ID);
			AbstractArtefact artefact = artefactAccessor.get(new ObjectId(artefactid));
			
			ArtefactInfo info = new ArtefactInfo();
			info.setName(artefact.getAttributes()!=null?artefact.getAttributes().get("name"):null);
			return info;
		} else {
			String respositoryId = ref.getRepositoryID();
			Repository repository = getRepository(respositoryId);
			return repository.getArtefactInfo(ref.getRepositoryParameters());
		}
	}
	
	
	public TestSetStatusOverview getReport(RepositoryObjectReference report) throws Exception {
		String respositoryId = report.getRepositoryID();
		Repository repository = getRepository(respositoryId);
		return repository.getTestSetStatusOverview(report.getRepositoryParameters());
	}
	
}
