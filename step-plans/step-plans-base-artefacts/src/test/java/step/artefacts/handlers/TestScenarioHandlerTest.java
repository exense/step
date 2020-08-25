package step.artefacts.handlers;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;

import junit.framework.Assert;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.Check;
import step.artefacts.TestScenario;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.threadpool.ThreadPoolPlugin;

public class TestScenarioHandlerTest {
	
	@Test
	public void test() throws IOException {
		Plan plan = PlanBuilder.create().startBlock(new TestScenario()).add(passedCheck()).add(passedCheck()).endBlock().build();
		
		StringWriter writer = new StringWriter();
		ExecutionEngine engine = ExecutionEngine.builder().withPlugin(new ThreadPoolPlugin()).withPlugin(new BaseArtefactPlugin()).build();
		engine.execute(plan).printTree(writer);
		
		Assert.assertTrue(writer.toString().startsWith("TestScenario:"+ReportNodeStatus.PASSED));
	}
	
	private Check passedCheck() {
		Check passedCheck = new Check();
		passedCheck.setExpression(new DynamicValue<Boolean>(true));
		return passedCheck;
	}

}
