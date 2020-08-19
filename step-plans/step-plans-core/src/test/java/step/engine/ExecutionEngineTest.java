package step.engine;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import junit.framework.Assert;
import step.artefacts.CheckArtefact;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.ExecutionEngineException;
import step.core.execution.OperationMode;
import step.core.execution.model.ExecutionParameters;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.core.plugins.Plugin;
import step.core.repositories.ArtefactInfo;
import step.core.repositories.ImportResult;
import step.core.repositories.Repository;
import step.core.repositories.RepositoryObjectReference;
import step.core.repositories.TestSetStatusOverview;
import step.engine.plugins.AbstractExecutionEnginePlugin;

public class ExecutionEngineTest {

	private static final String TEST_REPOSITORY = "testRepository";
	private static final String REPOSITORY_IMPORT_STATUS = "importStatus";
	private static final String REPOSITORY_IMPORT_STATUS_ERROR = "error";
	private static final String REPOSITORY_IMPORT_STATUS_SUCCESS = "success";

	@Test
	public void test() throws ExecutionEngineException, IOException {
		ExecutionEngine executionEngine = ExecutionEngine.builder().build();
		
		Plan plan = PlanBuilder.create().startBlock(new CheckArtefact()).endBlock().build();
		PlanRunnerResult result = executionEngine.execute(plan);
		
		Assert.assertEquals("CheckArtefact:PASSED:\n", result.getTreeAsString());
	}
	
	@Test
	public void testRepository() throws ExecutionEngineException, IOException {
		ExecutionEngine executionEngine = ExecutionEngine.builder().withPlugin(new TestRepositoryPlugin()).build();
		
		PlanRunnerResult result = executionEngine.execute(new ExecutionParameters(new RepositoryObjectReference(TEST_REPOSITORY, newSuccessfulRepositoryImport()), null));
		
		Assert.assertEquals("CheckArtefact:PASSED:\n", result.getTreeAsString());
	}
	
	@Test
	public void testRepositoryImportError() throws ExecutionEngineException, IOException {
		ExecutionEngine executionEngine = ExecutionEngine.builder().build();
		
		PlanRunnerResult result = executionEngine.execute(new ExecutionParameters(new RepositoryObjectReference(TEST_REPOSITORY, newFailingRepositoryImport()), null));
		
		//Assert.assertEquals(ExecutionStatus.ENDED, executionContext.getStatus());
		// Add assert of import error
	}

	protected HashMap<String, String> newFailingRepositoryImport() {
		HashMap<String, String> repositoryParameters = new HashMap<>();
		repositoryParameters.put(REPOSITORY_IMPORT_STATUS, REPOSITORY_IMPORT_STATUS_ERROR);
		return repositoryParameters;
	}
	
	protected HashMap<String, String> newSuccessfulRepositoryImport() {
		HashMap<String, String> repositoryParameters = new HashMap<>();
		repositoryParameters.put(REPOSITORY_IMPORT_STATUS, REPOSITORY_IMPORT_STATUS_SUCCESS);
		return repositoryParameters;
	}
	
	@Test
	public void testParentContext() throws ExecutionEngineException, IOException {
		ExecutionEngineContext parentContext = new ExecutionEngineContext(OperationMode.LOCAL);
		InMemoryPlanAccessor planAccessor = new InMemoryPlanAccessor();
		
		Plan plan = PlanBuilder.create().startBlock(new CheckArtefact(c->{
			Assert.assertTrue(c.getPlanAccessor()==planAccessor);
			c.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
		})).endBlock().build();
		parentContext.setPlanAccessor(planAccessor);
		
		ExecutionEngine executionEngine = ExecutionEngine.builder().withParentContext(parentContext).build();
		
		PlanRunnerResult result = executionEngine.execute(plan);
		
		Assert.assertEquals("CheckArtefact:PASSED:\n", result.getTreeAsString());
	}

	@Plugin
	public static class TestRepositoryPlugin extends AbstractExecutionEnginePlugin {
		

		@Override
		public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
			context.getRepositoryObjectManager().registerRepository(TEST_REPOSITORY, new Repository() {
				
				@Override
				public ImportResult importArtefact(ExecutionContext context, Map<String, String> repositoryParameters)
						throws Exception {
					ImportResult importResult = new ImportResult();

					if(repositoryParameters != null) {
						String importStatus = repositoryParameters.get(REPOSITORY_IMPORT_STATUS);
						if(importStatus.equals(REPOSITORY_IMPORT_STATUS_ERROR)) {
							importResult.setSuccessful(false);
						} else {
							importResult.setSuccessful(true);
							Plan plan = PlanBuilder.create().startBlock(new CheckArtefact()).endBlock().build();
							plan = context.getPlanAccessor().save(plan);
							importResult.setPlanId(plan.getId().toString());
						}
					}
					
					return importResult;
				}
				
				@Override
				public TestSetStatusOverview getTestSetStatusOverview(Map<String, String> repositoryParameters) throws Exception {
					return null;
				}
				
				@Override
				public ArtefactInfo getArtefactInfo(Map<String, String> repositoryParameters) throws Exception {
					return null;
				}
				
				@Override
				public void exportExecution(ExecutionContext context, Map<String, String> repositoryParameters) throws Exception {
				}
			});
		}

	}
}
