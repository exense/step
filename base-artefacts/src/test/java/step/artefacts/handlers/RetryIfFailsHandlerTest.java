package step.artefacts.handlers;

import static junit.framework.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

import step.artefacts.CheckArtefact;
import step.artefacts.IfBlock;
import step.artefacts.RetryIfFails;
import step.artefacts.SetVar;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;

public class RetryIfFailsHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void testSuccess() {
		setupContext();
		
		RetryIfFails block = add(new RetryIfFails());
		block.setMaxRetries("2");
		
		CheckArtefact check1 = addAsChildOf(new CheckArtefact(new Runnable() {
			@Override
			public void run() {
				ExecutionContext.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
			}
		}), block);
		
		execute(block);
		
		ReportNode child = getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		
		assertEquals(1, getChildren(child).size());
	}
	
	@Test
	public void testMaxRetry() {
		setupContext();
		
		RetryIfFails block = add(new RetryIfFails());
		block.setMaxRetries("2");
		block.setGracePeriod("1000");
		
		CheckArtefact check1 = addAsChildOf(new CheckArtefact(new Runnable() {
			@Override
			public void run() {
				ExecutionContext.getCurrentReportNode().setStatus(ReportNodeStatus.FAILED);
			}
		}), block);
				
		execute(block);
		
		ReportNode child = getFirstReportNode();
		Assert.assertTrue(child.getDuration()>=2000);
		assertEquals(child.getStatus(), ReportNodeStatus.FAILED);
		
		assertEquals(2, getChildren(child).size());
	}
	
	@Test
	public void testFalse() {
		setupContext();
		
		IfBlock block = add(new IfBlock("false"));
		addAsChildOf(new SetVar(), block);

		execute(block);
		
		ReportNode child = getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);	
		assertEquals(0, getChildren(child).size());
	}
}

