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
package step.engine;

import junit.framework.Assert;
import org.junit.Test;
import step.core.artefacts.CheckArtefact;
import step.core.artefacts.handlers.CheckArtefactHandler;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.*;
import step.core.execution.ExecutionEngine.Builder;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.core.repositories.*;
import step.engine.execution.ExecutionLifecycleManager;
import step.engine.execution.ExecutionVeto;
import step.engine.execution.ExecutionVetoer;
import step.engine.plugins.AbstractExecutionEnginePlugin;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ExecutionEngineTest {

	private static final String TEST_REPOSITORY = "testRepository";
	private static final String REPOSITORY_IMPORT_STATUS = "importStatus";
	private static final String REPOSITORY_IMPORT_STATUS_ERROR = "error";
	private static final String REPOSITORY_IMPORT_STATUS_SUCCESS = "success";
	public static final String VETO_TEST_2 = "VetoTest2";
	public static final String VETO_TEST_1 = "VetoTest1";

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
		assertEquals(ReportNodeStatus.IMPORT_ERROR, result.getResult());
	}

	@Test
	public void testExecutionVeto() throws ExecutionEngineException, IOException {
		ExecutionEngine executionEngine = ExecutionEngine.builder().withPlugin(new VetoingPlugin()).withPlugin(new TestRepositoryPlugin()).build();

		PlanRunnerResult result = executionEngine.execute(new ExecutionParameters(new RepositoryObjectReference(TEST_REPOSITORY, newSuccessfulRepositoryImport()), null));

		Execution execution = executionEngine.getExecutionEngineContext().getExecutionAccessor().get(result.getExecutionId());
		ImportResult importResult = execution.getImportResult();
		Assert.assertFalse(importResult.isSuccessful());
		Assert.assertEquals(List.of(VETO_TEST_1, VETO_TEST_2), importResult.getErrors());
		assertEquals(ReportNodeStatus.VETOED, result.getResult());
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
		
		Plan otherPlan = new Plan();
		planAccessor.save(otherPlan);
		
		Plan otherPlan2 = new Plan();
		
		Plan plan = PlanBuilder.create().startBlock(new CheckArtefact(c->{
			PlanAccessor localPlanAccessor = c.getPlanAccessor();
			Plan actual = localPlanAccessor.get(otherPlan.getId());
			// Assert that the plan "otherPlan" that has been saved to the parent context is available
			assertEquals(otherPlan.getId(), actual.getId());
			
			localPlanAccessor.save(otherPlan2);
			c.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
		})).endBlock().build();
		parentContext.setPlanAccessor(planAccessor);
		
		ExecutionEngine executionEngine = newExecutionEngineBuilder().withParentContext(parentContext).build();
		
		PlanRunnerResult result = executionEngine.execute(plan);
		
		// Assert that plan "otherPlan2" that has been saved within the execution to the plan accessor for the 
		// context has not be saved to the plan accessor of the parent context
		Plan actual = planAccessor.get(otherPlan2.getId());
		assertNull(actual);
		
		assertEquals(ReportNodeStatus.PASSED, result.getResult());
		Assert.assertEquals("CheckArtefact:PASSED:\n", result.getTreeAsString());
	}
	
	private Boolean exportCalled = false; 

	@Plugin
	@IgnoreDuringAutoDiscovery
	public class TestRepositoryPlugin extends AbstractExecutionEnginePlugin {
		
		@Override
		public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
			context.getRepositoryObjectManager().registerRepository(TEST_REPOSITORY, new AbstractRepository(Set.of()) {
				
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

	@Plugin
	@IgnoreDuringAutoDiscovery
	public class VetoingPlugin extends AbstractExecutionEnginePlugin implements ExecutionVetoer {
		@Override
		public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext) {
			executionContext.addExecutionVetoer(this);
		}

		@Override
		public List<ExecutionVeto> getExecutionVetoes(ExecutionContext context) {
			return List.of(new ExecutionVeto(VETO_TEST_1), new ExecutionVeto(VETO_TEST_2));
		}
	}

}
