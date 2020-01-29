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

import java.io.StringWriter;

import org.junit.Test;

import junit.framework.Assert;
import step.artefacts.Echo;
import step.artefacts.Sequence;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.DefaultPlanRunner;
import step.core.plans.runner.PlanRunner;
import step.core.plans.runner.PlanRunnerResult;

public class SequenceHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void test1ChildPassed() {		
		for(ReportNodeStatus status:ReportNodeStatus.values()) {
			test1Child(status);			
		};
	}

	private void test1Child(ReportNodeStatus status) {
		setupContext();
		
		Sequence block = new Sequence();
		block.addChild(newTestArtefact(status));

		execute(block);
		
		ReportNode child = getFirstReportNode();
		assertEquals(status, child.getStatus());
	}
	
	@Test
	public void test2ChildrenFailed() {
		setupContext();
		
		Sequence block = new Sequence();
		block.addChild(newTestArtefact(ReportNodeStatus.PASSED));
		block.addChild(newTestArtefact(ReportNodeStatus.FAILED));

		execute(block);
		
		ReportNode child = getFirstReportNode();
		assertEquals(ReportNodeStatus.FAILED, child.getStatus());
	}
	
	@Test
	public void test2ChildrenTechError() {
		setupContext();
		
		Sequence block = new Sequence();
		block.addChild(newTestArtefact(ReportNodeStatus.PASSED));
		block.addChild(newTestArtefact(ReportNodeStatus.TECHNICAL_ERROR));

		execute(block);
		
		ReportNode child = getFirstReportNode();
		assertEquals(ReportNodeStatus.TECHNICAL_ERROR, child.getStatus());
	}
	
	@Test
	public void testDefaultContinueOnError() {
		setupContext();
		
		Sequence block = new Sequence();
		block.addChild(newTestArtefact(ReportNodeStatus.TECHNICAL_ERROR));
		block.addChild(newTestArtefact(ReportNodeStatus.PASSED));

		execute(block);
		
		ReportNode child = getFirstReportNode();
		assertEquals(ReportNodeStatus.TECHNICAL_ERROR, child.getStatus());
		assertEquals(1, getChildren(child).size());
	}
	
	@Test
	public void testDefaultContinueOnError_2() {
		setupContext();
		
		Sequence block = new Sequence();
		block.addChild(newTestArtefact(ReportNodeStatus.PASSED));
		block.addChild(newTestArtefact(ReportNodeStatus.TECHNICAL_ERROR));

		execute(block);
		
		ReportNode child = getFirstReportNode();
		assertEquals(ReportNodeStatus.TECHNICAL_ERROR, child.getStatus());
		assertEquals(2, getChildren(child).size());
	}
	
	@Test
	public void testContinueOnError() {
		setupContext();
		
		Sequence block = new Sequence();
		block.setContinueOnError(new DynamicValue<>(true));
		block.addChild(newTestArtefact(ReportNodeStatus.TECHNICAL_ERROR));
		block.addChild(newTestArtefact(ReportNodeStatus.PASSED));

		execute(block);
		
		ReportNode child = getFirstReportNode();
		assertEquals(ReportNodeStatus.TECHNICAL_ERROR, child.getStatus());
		assertEquals(2, getChildren(child).size());
	}
	
	@Test
	public void testContinueOnError_2() {
		setupContext();
		
		Sequence block = new Sequence();
		block.setContinueOnError(new DynamicValue<>(true));
		block.addChild(newTestArtefact(ReportNodeStatus.FAILED));
		block.addChild(newTestArtefact(ReportNodeStatus.PASSED));

		execute(block);
		
		ReportNode child = getFirstReportNode();
		assertEquals(ReportNodeStatus.FAILED, child.getStatus());
		assertEquals(2, getChildren(child).size());
	}
	
	@Test
	public void testNotContinueOnError() {
		setupContext();
		
		Sequence block = new Sequence();
		block.setContinueOnError(new DynamicValue<>(false));
		block.addChild(newTestArtefact(ReportNodeStatus.FAILED));
		block.addChild(newTestArtefact(ReportNodeStatus.PASSED));

		execute(block);
		
		ReportNode child = getFirstReportNode();
		assertEquals(ReportNodeStatus.FAILED, child.getStatus());
		assertEquals(1, getChildren(child).size());
	}
	
	@Test
	public void testPacingAsInteger() throws Exception {
		// Create a sequence block with a pacing defined as an Integer
		Integer pacing = 500;
		Sequence block = new Sequence();
		block.setPacing(new DynamicValue<>("500", ""));
	
		Echo echo = new Echo();
		echo.setText(new DynamicValue<>("'This is a test'", ""));
		
		// Create a plan with this sequence block
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
			
		Assert.assertEquals("Sequence:PASSED:\n" + 
				" Echo:PASSED:\n" +
				"" , writer.toString());				
	}
	
	@Test
	public void testPacingAsLong() throws Exception {
		// Create a sequence block with a pacing defined as a Long
		Long pacing = 500l;
		Sequence block = new Sequence();
		block.setPacing(new DynamicValue<>(pacing+"l", ""));
	
		Echo echo = new Echo();
		echo.setText(new DynamicValue<>("'This is a test'", ""));
		
		// Create a plan with this sequence block
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
			
		Assert.assertEquals("Sequence:PASSED:\n" + 
				" Echo:PASSED:\n" +
				"" , writer.toString());				
	}	
	
	@Test
	public void testEmptyPacing() throws Exception {
		// Create a sequence block with a an empty pacing value
		Sequence block = new Sequence();
		block.setPacing(new DynamicValue<>("", ""));
	
		Echo echo = new Echo();
		echo.setText(new DynamicValue<>("'This is a test'", ""));
		
		// Create a plan with this sequence block
		Plan plan = PlanBuilder.create()
				.startBlock(block).add(echo)
				.endBlock()
				.build();
		
		// Run the plan
		PlanRunner planRunner = new DefaultPlanRunner();
		PlanRunnerResult result = planRunner.run(plan);	
		
		// Print the report tree and assert it matches the expected report
		StringWriter writer = new StringWriter();
		result.printTree(writer);
			
		Assert.assertEquals("Sequence:PASSED:\n" + 
				" Echo:PASSED:\n" +
				"" , writer.toString());				
	}	
}