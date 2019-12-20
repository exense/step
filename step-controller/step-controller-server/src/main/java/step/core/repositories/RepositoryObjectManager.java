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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.artefacts.CallPlan;
import step.artefacts.TestCase;
import step.artefacts.TestSet;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactAccessor;
import step.core.artefacts.ArtefactRegistry;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
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

	public ImportResult importArtefact(ExecutionContext context, RepositoryObjectReference artefact) throws Exception  {
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

		String respositoryId = report.getRepositoryID();
		if(!report.getRepositoryID().equals(LOCAL)) {
			Repository repository = getRepository(respositoryId);
			
			try {
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
	
	private static final String LOCAL = "local";
	private static final String ARTEFACT_ID = "artefactid";
	
	public ArtefactInfo getArtefactInfo(RepositoryObjectReference ref) throws Exception {
		try {
			if(ref.getRepositoryID().equals(LOCAL)) {
				String artefactid = ref.getRepositoryParameters().get(ARTEFACT_ID);
				AbstractArtefact artefact = artefactAccessor.get(new ObjectId(artefactid));
				
				ArtefactInfo info = new ArtefactInfo();
				info.setName(artefact.getAttributes()!=null?artefact.getAttributes().get("name"):null);
				info.setType(ArtefactRegistry.getArtefactName(artefact.getClass()));
				return info;
			} else {
				String respositoryId = ref.getRepositoryID();
				Repository repository = getRepository(respositoryId);
				return repository.getArtefactInfo(ref.getRepositoryParameters());
			}
		} catch (Exception e) {
			logger.error("Error while getting artefact infos for " + ref,e);
			throw e;
		}
	}
	
	
	public TestSetStatusOverview getReport(RepositoryObjectReference report) throws Exception {
		if(!report.getRepositoryID().equals(LOCAL)) {
			String respositoryId = report.getRepositoryID();
			Repository repository = getRepository(respositoryId);
			return repository.getTestSetStatusOverview(report.getRepositoryParameters());
		}  else {
			TestSetStatusOverview testSetStatusOverview = new TestSetStatusOverview();

			String artefactid = report.getRepositoryParameters().get(ARTEFACT_ID);
			AbstractArtefact artefact = artefactAccessor.get(new ObjectId(artefactid));

			if(artefact instanceof TestSet) {
				// Perform a very basic parsing of the artefact tree to get a list of test cases referenced 
				// in this test set. Only direct children of the root node are considered 
				Iterator<AbstractArtefact> children = artefactAccessor.getChildren(artefact);
				children.forEachRemaining(child->{
					if(child instanceof TestCase) {
						addTestRunStatus(testSetStatusOverview.getRuns(), child);
					} else if(child instanceof CallPlan) {
						AbstractArtefact referencedArtefact = artefactAccessor.get(((CallPlan)child).getArtefactId());
						if(referencedArtefact instanceof TestCase) {
							addTestRunStatus(testSetStatusOverview.getRuns(), referencedArtefact);
						}
					}
				});
			}
			return testSetStatusOverview;
		}
	}
	
	private void addTestRunStatus(List<TestRunStatus> testRunStatusList, AbstractArtefact artefact) {
		testRunStatusList.add(new TestRunStatus(artefact.getAttributes().get(AbstractOrganizableObject.NAME), ReportNodeStatus.NORUN));
	}
	
}
