/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.artefacts.handlers;

import static junit.framework.Assert.assertEquals;
import static step.planbuilder.BaseArtefacts.sequence;
import static step.planbuilder.BaseArtefacts.set;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import junit.framework.Assert;
import step.artefacts.CheckArtefact;
import step.artefacts.Echo;
import step.artefacts.Set;
import step.artefacts.Sleep;
import step.artefacts.While;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.DefaultPlanRunner;
import step.core.plans.runner.PlanRunner;
import step.core.plans.runner.PlanRunnerResult;

public class WhileHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void testTrue() {
		setupContext();
		
		While block = new While("true");
		block.setMaxIterations(new DynamicValue<>(2));
		Set set = new Set();
		block.addChild(set);

		execute(block);
		
		ReportNode child = getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		
		ReportNode sequence = getChildren(child).get(1);
		ReportNode setNode =  getChildren(sequence).get(0);
		assertEquals(set.getId(), setNode.getArtefactID());
		assertEquals(setNode.getStatus(), ReportNodeStatus.PASSED);
	}
	
	@Test
	public void testFalse() {
		setupContext();
		
		While block = new While("false");
		block.setMaxIterations(new DynamicValue<>(1));

		execute(block);
		
		ReportNode child = getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);	
		assertEquals(0, getChildren(child).size());
	}
	
	@Test
	public void testPostCondition() throws IOException {
		// Create a while block with a post condition
		While block = new While("");
		block.setPostCondition(new DynamicValue<>("i<=2", ""));
		block.setMaxIterations(new DynamicValue<>(10));

		// Create a plan with this while block
		Plan plan = PlanBuilder.create().startBlock(sequence())
											.add(set("i", "0"))
											.startBlock(block)
												.add(set("i", "i+1"))
											.endBlock()
										.endBlock().build();
		
		// Run the plan
		PlanRunner planRunner = new DefaultPlanRunner();
		PlanRunnerResult result = planRunner.run(plan);
		
		// Print the report tree and assert it matches the expected report
		StringWriter writer = new StringWriter();
		result.printTree(writer);
		
		Assert.assertEquals("Sequence:PASSED:\n" + 
				" Set:PASSED:\n" + 
				" While:PASSED:\n" + 
				"  Iteration_0:PASSED:\n" + 
				"   Set:PASSED:\n" + 
				"  Iteration_1:PASSED:\n" + 
				"   Set:PASSED:\n" + 
				"  Iteration_2:PASSED:\n" + 
				"   Set:PASSED:\n" + 
				"" ,writer.toString());
	}
	
	@Test
	public void testPacingAsInteger() throws Exception {
		// Create a sequence block with a pacing defined as an Integer
		Integer pacing = 500;
		While block = new While();
		block.setPacing(new DynamicValue<>(pacing.toString(), ""));
		block.setMaxIterations(new DynamicValue<>(1));

		Echo echo = new Echo();
		echo.setText(new DynamicValue<>("'This is a test'", ""));
		
		// Create a plan with this while block
		Plan plan = PlanBuilder.create()
				.startBlock(block).add(echo)
				.endBlock()
				.build();
		
		// Run the plan
		PlanRunner planRunner = new DefaultPlanRunner();
		
		// Get start time
		Long startTime = System.currentTimeMillis();
		PlanRunnerResult result = planRunner.run(plan);
		Long duration = System.currentTimeMillis() - startTime;
		Assert.assertTrue("Execution took less time than defined pacing", duration >= pacing);
		
		// Print the report tree and assert it matches the expected report
		StringWriter writer = new StringWriter();
		result.printTree(writer);
				
		Assert.assertEquals("While:PASSED:\n" + 
				" Iteration_0:PASSED:\n" +
				"  Echo:PASSED:\n" +
				"", writer.toString());	
	}
	
	@Test
	public void testPacingAsLong() throws Exception {
		// Create a sequence block with a pacing defined as a Long
		Long pacing = 500l;
		While block = new While();
		block.setPacing(new DynamicValue<>(pacing+"l", ""));
		block.setMaxIterations(new DynamicValue<>(1));

		Echo echo = new Echo();
		echo.setText(new DynamicValue<>("'This is a test'", ""));
		
		// Create a plan with this while block
		Plan plan = PlanBuilder.create()
				.startBlock(block).add(echo)
				.endBlock()
				.build();
		
		// Run the plan
		PlanRunner planRunner = new DefaultPlanRunner();
		
		// Get start time
		Long startTime = System.currentTimeMillis();
		PlanRunnerResult result = planRunner.run(plan);
		Long duration = System.currentTimeMillis() - startTime;
		Assert.assertTrue("Execution took less time than defined pacing", duration >= pacing);
		
		// Print the report tree and assert it matches the expected report
		StringWriter writer = new StringWriter();
		result.printTree(writer);
				
		Assert.assertEquals("While:PASSED:\n" + 
				" Iteration_0:PASSED:\n" +
				"  Echo:PASSED:\n" +
				"", writer.toString());	
	}
	
	@Test
	public void testTimeoutExceeded() throws Exception {
		long timeout = 50l;
		
		// As Long
		AtomicInteger count = new AtomicInteger(0);
		StringWriter writer = testTimeout("100l", timeout, count);
		Assert.assertTrue(writer.toString().startsWith("While:"+ReportNodeStatus.PASSED));
		Assert.assertTrue(count.get()<10);
		
		// As Integer
		count = new AtomicInteger(0);
		writer = testTimeout("100", timeout, count);
		Assert.assertTrue(writer.toString().startsWith("While:"+ReportNodeStatus.PASSED));
		Assert.assertTrue(count.get()<10);
	}
	
	@Test
	public void testTimeoutDefault() throws Exception {
		// As Long
		AtomicInteger count = new AtomicInteger(0);
		StringWriter writer = testTimeout("100l" ,50, count);
		Assert.assertTrue(writer.toString().startsWith("While:"+ReportNodeStatus.PASSED));
		Assert.assertTrue(count.get()<=10);
		
		// As Integer
		count = new AtomicInteger(0);
		writer = testTimeout("100", 50, count);
		Assert.assertTrue(writer.toString().startsWith("While:"+ReportNodeStatus.PASSED));
		Assert.assertTrue(count.get()<=10);
	}
	
	@Test
	public void testEmptyTimeoutDefault() throws Exception {
		// Empty, so no timeout
		AtomicInteger count = new AtomicInteger(0);
		StringWriter writer = testTimeout("" ,10, count);
		Assert.assertTrue(writer.toString().startsWith("While:"+ReportNodeStatus.PASSED));
		Assert.assertTrue(count.get()==10);
	}
	
	private StringWriter testTimeout(String timeoutExpresssion, long sleepDuration, AtomicInteger count) throws IOException {
		While artefact = new While();
		artefact.setCondition(new DynamicValue<Boolean>(true));
		artefact.setTimeout(new DynamicValue<>(timeoutExpresssion, ""));
		artefact.setMaxIterations(new DynamicValue<Integer>(10));
		
		Sleep sleep = new Sleep();
		sleep.setDuration(new DynamicValue<Long>(sleepDuration));
		
		CheckArtefact check = new CheckArtefact(c-> {
			count.incrementAndGet();
		});
		
		Plan plan = PlanBuilder.create().startBlock(artefact).add(sleep).add(check).endBlock().build();
		DefaultPlanRunner runner = new DefaultPlanRunner();
		
		StringWriter writer = new StringWriter();
		runner.run(plan).printTree(writer);
		return writer;
	}		
}