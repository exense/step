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
import step.core.repositories.ArtefactInfo;
import step.core.repositories.ImportResult;
import step.core.repositories.Repository;
import step.core.repositories.RepositoryObjectReference;
import step.core.repositories.TestRunStatus;
import step.core.repositories.TestSetStatusOverview;

public class LocalRepository implements Repository {

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
