package step.artefacts.handlers;

import static junit.framework.Assert.assertEquals;

import java.util.HashSet;

import org.junit.Test;

import step.artefacts.CheckArtefact;
import step.artefacts.TestSet;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;

public class TestSetHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void testParallel() throws Exception {
		HashSet<Long> threadIdSet = new HashSet<>();

		execute(threadIdSet, "10", "DummyName{childID}");
		
		assertEquals("Ensure that the TestSetHandler runs the child artefacts in parallel", 10, threadIdSet.size());
	}
	
	@Test
	public void testSequential() throws Exception {
		HashSet<Long> threadIdSet = new HashSet<>();

		execute(threadIdSet, "1", "DummyName{childID}");
		
		assertEquals("Ensure that the TestSetHandler runs the child artefacts sequentially", 1, threadIdSet.size());
	}

	private void execute(HashSet<Long> threadIdSet, String tecExecutionThreads, String childNamePattern) {
		setupContext();
		
		ExecutionContext.getCurrentContext().getVariablesManager().putVariable(
				ExecutionContext.getCurrentContext().getReport(), "tec.execution.threads", tecExecutionThreads);
		
		ExecutionContext.getCurrentContext().getVariablesManager().getVariable("var");
		
		TestSet set = new TestSet();
		add(set);		
		
		int nChilds = 20;
		
		for(int j=0;j<nChilds;j++) {
			addAsChildOf(new CheckArtefact(new Runnable() {
				@Override
				public void run() {
					synchronized (threadIdSet) {
						threadIdSet.add(Thread.currentThread().getId());						
					}
					ExecutionContext.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
				}
			}), set);			
		}
		
		createSkeleton(set);
		execute(set);
		
		ReportNode child = getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		
		assertEquals(nChilds, getChildren(child).size());
		
		for(ReportNode node:getChildren(child)) {
			assertEquals(node.getStatus(), ReportNodeStatus.PASSED);					
		}
	}
}

