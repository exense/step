package step.engine;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import junit.framework.Assert;
import step.core.artefacts.CheckArtefact;
import step.core.artefacts.handlers.CheckArtefactHandler;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngine.Builder;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.ExecutionEngineException;
import step.core.execution.OperationMode;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.core.repositories.ArtefactInfo;
import step.core.repositories.ImportResult;
import step.core.repositories.Repository;
import step.core.repositories.RepositoryObjectReference;
import step.core.repositories.TestSetStatusOverview;
import step.engine.execution.ExecutionLifecycleManager;
import step.engine.plugins.AbstractExecutionEnginePlugin;

public class ExecutionEngineTest {

	private static final String TEST_REPOSITORY = "testRepository";
	private static final String REPOSITORY_IMPORT_STATUS = "importStatus";
	private static final String REPOSITORY_IMPORT_STATUS_ERROR = "error";
	private static final String REPOSITORY_IMPORT_STATUS_SUCCESS = "success";

	@Test
	public void test() throws ExecutionEngineException, IOException {
		ExecutionEngine executionEngine = newExecutionEngine();
		
		Plan plan = PlanBuilder.create().startBlock(new CheckArtefact()).endBlock().build();
		PlanRunnerResult result = executionEngine.execute(plan);
		
		Assert.assertEquals("CheckArtefact:PASSED:\n", result.getTreeAsString());
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
	}

	protected ExecutionEngine newExecutionEngine() {
		return newExecutionEngineBuilder().build();
	}

	protected Builder newExecutionEngineBuilder() {
		return ExecutionEngine.builder().withPlugin(new AbstractExecutionEnginePlugin() {

			@Override
			public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext,
					ExecutionEngineContext executionEngineContext) {
				executionEngineContext.getArtefactHandlerRegistry().put(CheckArtefact.class, CheckArtefactHandler.class);
			}
			
		});
	}
	
	@Test
	public void test2PhasesExecution() throws ExecutionEngineException, IOException {
		ExecutionEngine executionEngine = newExecutionEngine();
		
		Plan plan = PlanBuilder.create().startBlock(new CheckArtefact()).endBlock().build();
		String executionId = executionEngine.initializeExecution(new ExecutionParameters(plan, null));
		PlanRunnerResult result = executionEngine.execute(executionId);
		
		Assert.assertEquals("CheckArtefact:PASSED:\n", result.getTreeAsString());
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
	}
	
	@Test
	public void testAbortExecution() throws ExecutionEngineException, IOException, TimeoutException, InterruptedException, ExecutionException {
		ExecutionEngine executionEngine = newExecutionEngine();

		Semaphore semaphore = new Semaphore(1);
		semaphore.acquire();
		Plan plan = PlanBuilder.create().startBlock(new CheckArtefact(e-> {
			// Notify execution start
			semaphore.release();
			while(!e.isInterrupted()) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e1) {}
			}
		})).endBlock().build();
		
		Future<PlanRunnerResult> future = Executors.newSingleThreadExecutor().submit(()->executionEngine.execute(plan));

		// Wait for the execution to start
		semaphore.tryAcquire(10, TimeUnit.SECONDS);
		List<ExecutionContext> currentExecutions = executionEngine.getCurrentExecutions();
		
		// The number of executions should now be 1
		Assert.assertEquals(1, currentExecutions.size());
		ExecutionContext executionContext = currentExecutions.get(0);
		
		// Abort the execution
		new ExecutionLifecycleManager(executionContext).abort();
		
		// Wait for the execution to terminate
		PlanRunnerResult result = future.get();
		result.waitForExecutionToTerminate(1000);
		
		// The number of executions should now be 0
		currentExecutions = executionEngine.getCurrentExecutions();
		Assert.assertEquals(0, currentExecutions.size());
		
		// TODO the status is reported here as RUNNING which is wrong. Fix this in the future
		assertEquals(ReportNodeStatus.RUNNING, result.getResult());
	}
	
	@Test
	public void testRepository() throws ExecutionEngineException, IOException {
		ExecutionEngine executionEngine = newExecutionEngineBuilder().withPlugin(new TestRepositoryPlugin()).build();
		
		PlanRunnerResult result = executionEngine.execute(new ExecutionParameters(new RepositoryObjectReference(TEST_REPOSITORY, newSuccessfulRepositoryImport()), null));
		
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
		Assert.assertEquals("CheckArtefact:PASSED:\n", result.getTreeAsString());
		Assert.assertTrue(exportCalled);
	}
	
	@Test
	public void testSimulationMode() throws ExecutionEngineException, IOException {
		ExecutionEngine executionEngine = newExecutionEngineBuilder().withPlugin(new TestRepositoryPlugin()).build();
		
		// Build executionParameters with Simulation mode
		ExecutionParameters executionParameters = new ExecutionParameters(ExecutionMode.SIMULATION, null, new RepositoryObjectReference(TEST_REPOSITORY, newSuccessfulRepositoryImport()), null, null, null, null, false, null);
		PlanRunnerResult result = executionEngine.execute(executionParameters); 
		
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
		Assert.assertEquals("CheckArtefact:PASSED:\n", result.getTreeAsString());
		Assert.assertFalse(exportCalled);
	}
	
	@Test
	public void testRepositoryImportError() throws ExecutionEngineException, IOException {
		ExecutionEngine executionEngine = ExecutionEngine.builder().withPlugin(new TestRepositoryPlugin()).build();
		
		PlanRunnerResult result = executionEngine.execute(new ExecutionParameters(new RepositoryObjectReference(TEST_REPOSITORY, newFailingRepositoryImport()), null));
		
		Execution execution = executionEngine.getExecutionEngineContext().getExecutionAccessor().get(result.getExecutionId());
		ImportResult importResult = execution.getImportResult();
		Assert.assertFalse(importResult.isSuccessful());
		String string = importResult.getErrors().get(0);
		Assert.assertEquals(REPOSITORY_IMPORT_STATUS_ERROR, string);
		assertEquals(ReportNodeStatus.NORUN, result.getResult());
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
		
		ExecutionEngine executionEngine = newExecutionEngineBuilder().withParentContext(parentContext).build();
		
		PlanRunnerResult result = executionEngine.execute(plan);
		
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
		Assert.assertEquals("CheckArtefact:PASSED:\n", result.getTreeAsString());
	}
	
	private Boolean exportCalled = false; 

	@Plugin
	@IgnoreDuringAutoDiscovery
	public class TestRepositoryPlugin extends AbstractExecutionEnginePlugin {
		
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
							List<String> errors = new ArrayList<>();
							errors.add(REPOSITORY_IMPORT_STATUS_ERROR);
							importResult.setErrors(errors);
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
					exportCalled = true;
				}
			});
		}
	}
}
