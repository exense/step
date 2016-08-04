package step.artefacts.handlers;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import step.artefacts.Case;
import step.artefacts.IfBlock;
import step.artefacts.Switch;
import step.artefacts.Set;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;

public class SwitchHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void testTrue() {
		setupContext();
		
		ExecutionContext.getCurrentContext().getVariablesManager().putVariable(
				ExecutionContext.getCurrentContext().getReport(), "var", "val1");
		
		ExecutionContext.getCurrentContext().getVariablesManager().getVariable("var");
		
		Switch select = new Switch();
		select.setExpression("'val1'");
		add(select);
		
		Case c1 = new Case();
		c1.setValue("val1");
		addAsChildOf(c1, select);
		
		Set set1 = addAsChildOf(new Set(), c1);
		
		Case c2 = new Case();
		c2.setValue("val2");
		addAsChildOf(c1, select);
		
		Set set2 = addAsChildOf(new Set(), c1);
		
		execute(select);
		
		ReportNode child = getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		
		assertEquals(getChildren(child).size(), 1);
		
		ReportNode case1 = getChildren(child).get(0);
		assertEquals(c1.getId(), case1.getArtefactID());
		assertEquals(case1.getStatus(), ReportNodeStatus.PASSED);
	}
	
	@Test
	public void testFalse() {
		setupContext();
		
		IfBlock block = add(new IfBlock("false"));
		addAsChildOf(new Set(), block);

		execute(block);
		
		ReportNode child = getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);	
		assertEquals(0, getChildren(child).size());
	}
}

