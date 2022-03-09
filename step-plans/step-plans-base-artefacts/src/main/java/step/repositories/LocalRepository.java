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
package step.repositories;

import java.util.List;
import java.util.Map;

import step.artefacts.CallPlan;
import step.artefacts.TestCase;
import step.artefacts.TestSet;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.repositories.*;

public class LocalRepository extends AbstractRepository {

	private final PlanAccessor planAccessor;
	
	public LocalRepository(PlanAccessor planAccessor) {
		super();
		this.planAccessor = planAccessor;
	}

	@Override
	public ArtefactInfo getArtefactInfo(Map<String, String> repositoryParameters) throws Exception {
		String planId = repositoryParameters.get(RepositoryObjectReference.PLAN_ID);
		Plan plan = planAccessor.get(planId);
		
		ArtefactInfo info = new ArtefactInfo();
		info.setName(plan.getAttributes()!=null?plan.getAttributes().get(AbstractOrganizableObject.NAME):null);
		info.setType(AbstractArtefact.getArtefactName(plan.getRoot().getClass()));
		return info;
	}

	@Override
	public TestSetStatusOverview getTestSetStatusOverview(Map<String, String> repositoryParameters) throws Exception {
		TestSetStatusOverview testSetStatusOverview = new TestSetStatusOverview();

		String planId = repositoryParameters.get(RepositoryObjectReference.PLAN_ID);
		Plan plan = planAccessor.get(planId);

		AbstractArtefact rootArtefact = plan.getRoot();
		
		if(rootArtefact instanceof TestSet) {
			// Perform a very basic parsing of the artefact tree to get a list of test cases referenced 
			// in this test set. Only direct children of the root node are considered 
			List<AbstractArtefact> children = rootArtefact.getChildren();
			children.forEach(child->{
				if(child instanceof TestCase) {
					addTestRunStatus(testSetStatusOverview.getRuns(), child);
				} else if(child instanceof CallPlan) {
					Plan referencedPlan = planAccessor.get(((CallPlan)child).getPlanId());
					AbstractArtefact root = referencedPlan.getRoot();
					if(root instanceof TestCase) {
						addTestRunStatus(testSetStatusOverview.getRuns(), root);
					}
				}
			});
		}
		return testSetStatusOverview;
	}
	
	private void addTestRunStatus(List<TestRunStatus> testRunStatusList, AbstractArtefact abstractArtefact) {
		testRunStatusList.add(new TestRunStatus(abstractArtefact.getId().toString(), 
				abstractArtefact.getAttributes().get(AbstractOrganizableObject.NAME), ReportNodeStatus.NORUN));
	}

	@Override
	public ImportResult importArtefact(ExecutionContext context, Map<String, String> repositoryParameters)
			throws Exception {
		ImportResult importResult = new ImportResult();
		String planId = context.getExecutionParameters().getRepositoryObject().getRepositoryParameters().get(RepositoryObjectReference.PLAN_ID);
		importResult.setPlanId(planId);
		importResult.setSuccessful(true);
		PlanAccessor contextPlanAccessor = context.getPlanAccessor();
		if(contextPlanAccessor.get(planId) == null) {
			contextPlanAccessor.save(planAccessor.get(planId));
		}
		return importResult;
	}

	@Override
	public void exportExecution(ExecutionContext context, Map<String, String> repositoryParameters) throws Exception {
		// The local repository doesn't perform any export
	}
}
