package step.artefacts.handlers;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import step.artefacts.IfBlock;
import step.artefacts.SetVar;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.variables.VariablesManager;

public class SetVarHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void test() {
		setupContext();
		
		SetVar set = add(new SetVar());
		set.setset0("var='val1'");
		set.setset1("{var2}='val2'");
		//set.setset2("var3={var}");
		set.setset2("var3=var");

		execute(set);
		
		VariablesManager v = ExecutionContext.getCurrentContext().getVariablesManager();
		
		assertEquals("val1",v.getVariable("var"));
		assertEquals("val2",v.getVariable("var2"));
		assertEquals("val1",v.getVariable("var3"));
		
		
		ReportNode child = getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
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

