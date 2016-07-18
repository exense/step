package step.artefacts.handlers;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import step.artefacts.IfBlock;
import step.artefacts.SetVar;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;

public class IfBlockHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void testTrue() {
		setupContext();
		
		IfBlock block = add(new IfBlock("true"));
		SetVar set = addAsChildOf(new SetVar(), block);

		execute(block);
		
		ReportNode child = getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		
		ReportNode setNode = getChildren(child).get(0);
		assertEquals(set.getId(), setNode.getArtefactID());
		assertEquals(setNode.getStatus(), ReportNodeStatus.PASSED);
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

