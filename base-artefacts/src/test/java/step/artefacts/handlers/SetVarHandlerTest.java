package step.artefacts.handlers;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import step.artefacts.Set;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.variables.VariablesManager;

public class SetVarHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void test() {
		setupContext();
		
		Set set = add(new Set());
		set.setKey("var");
		set.setExpression("'val1'");

		execute(set);
		
		VariablesManager v = ExecutionContext.getCurrentContext().getVariablesManager();
		
		assertEquals("val1",v.getVariable("var"));

		ReportNode child = getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
	}
}

