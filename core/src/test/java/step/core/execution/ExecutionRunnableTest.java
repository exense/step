package step.core.execution;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import java.util.ArrayList;

import org.junit.Test;
import org.mockito.Mockito;

import step.artefacts.CheckArtefact;
import step.core.GlobalContext;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;
import step.core.plugins.AbstractPlugin;
import step.core.repositories.RepositoryObjectManager;
import step.core.repositories.RepositoryObjectReference;

public class ExecutionRunnableTest {

	@Test 
	public void test() {
		GlobalContext globalContext = ExecutionTestHelper.createGlobalContext();
		globalContext.getPluginManager().register(new AbstractPlugin() {

			@Override
			public void executionStart(ExecutionContext context) {
				context.getVariablesManager().putVariable(context.getReport(), "tec.execution.exports", "[]");
				super.executionStart(context);
			}
		});
		
		CheckArtefact artefact = new CheckArtefact(new Runnable() {
			@Override
			public void run() {
				ExecutionContext.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
			}
		});
		globalContext.getArtefactAccessor().save(artefact);

		RepositoryObjectManager repo = Mockito.mock(RepositoryObjectManager.class);		
		Mockito.when(repo.importArtefact(Mockito.anyObject())).thenReturn(artefact.getId().toString());
		globalContext.setRepositoryObjectManager(repo);
		
		ExecutionRunnableFactory f = new ExecutionRunnableFactory(globalContext);
		
		ExecutionParameters p = new ExecutionParameters("user",null, ExecutionMode.RUN);
		p.setArtefact(new RepositoryObjectReference());
		p.setExports(new ArrayList<>());
		Execution e = f.createExecution(p, null);
		
		ExecutionRunnable r = f.newExecutionRunnable(e);

		r.run();
		
		Execution execution = globalContext.getExecutionAccessor().get(e.getId());
		assertNotNull(execution);
		assertNotNull(execution.getStartTime());
		assertNotNull(execution.getEndTime());
		assertEquals(ExecutionStatus.ENDED,execution.getStatus());
		assertNull(execution.getExecutionTaskID());
		
		ReportNode node = globalContext.getReportAccessor().getReportNodesByExecutionIDAndArtefactID(e.getId(), artefact.getId().toString()).next();
		assertNotNull(node);
		assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		
	}
	
}
