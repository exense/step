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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.artefacts.CallPlan;
import step.artefacts.TestCase;
import step.artefacts.TestSet;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactRegistry;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.execution.model.ReportExport;
import step.core.execution.model.ReportExportStatus;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;

public class RepositoryObjectManager {

	private static final Logger logger = LoggerFactory.getLogger(RepositoryObjectManager.class);
	
	public static final String CLIENT_KEY = "RepositoryObjectManager_Client";
	
	private Map<String, Repository> repositories = new ConcurrentHashMap<>();
	
	private PlanAccessor planAccessor;
	
	public RepositoryObjectManager(PlanAccessor planAccessor) {
		super();
		this.planAccessor = planAccessor;
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
	
	public ArtefactInfo getArtefactInfo(RepositoryObjectReference ref) throws Exception {
		try {
			if(ref.getRepositoryID().equals(LOCAL)) {
				String planId = ref.getRepositoryParameters().get(RepositoryObjectReference.PLAN_ID);
				Plan plan = planAccessor.get(planId);
				
				ArtefactInfo info = new ArtefactInfo();
				info.setName(plan.getAttributes()!=null?plan.getAttributes().get(AbstractOrganizableObject.NAME):null);
				info.setType(ArtefactRegistry.getArtefactName(plan.getRoot().getClass()));
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

			String planId = report.getRepositoryParameters().get(RepositoryObjectReference.PLAN_ID);
			Plan plan = planAccessor.get(planId);

			AbstractArtefact rootArtefact = plan.getRoot();
			
			if(rootArtefact instanceof TestSet) {
				// Perform a very basic parsing of the artefact tree to get a list of test cases referenced 
				// in this test set. Only direct children of the root node are considered 
				List<AbstractArtefact> children = rootArtefact.getChildren();
				children.forEach(child->{
					if(child instanceof TestCase) {
						addTestRunStatus(testSetStatusOverview.getRuns(), child.getAttributes().get(AbstractOrganizableObject.NAME));
					} else if(child instanceof CallPlan) {
						Plan referencedPlan = planAccessor.get(((CallPlan)child).getPlanId());
						if(referencedPlan.getRoot() instanceof TestCase) {
							addTestRunStatus(testSetStatusOverview.getRuns(), referencedPlan.getAttributes().get(AbstractOrganizableObject.NAME));
						}
					}
				});
			}
			return testSetStatusOverview;
		}
	}
	
	private void addTestRunStatus(List<TestRunStatus> testRunStatusList, String name) {
		testRunStatusList.add(new TestRunStatus(name, ReportNodeStatus.NORUN));
	}
	
}
