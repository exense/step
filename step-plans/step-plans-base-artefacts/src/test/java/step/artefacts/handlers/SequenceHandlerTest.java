/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
 ******************************************************************************/
package step.artefacts.handlers;

import static org.junit.Assert.assertEquals;
import static step.planbuilder.BaseArtefacts.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import org.junit.Assert;
import step.artefacts.Check;
import step.artefacts.Echo;
import step.artefacts.Sequence;
import step.core.artefacts.CheckArtefact;
import step.core.artefacts.ChildrenBlock;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionStatus;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.DefaultPlanRunner;
import step.core.plans.runner.PlanRunner;
import step.core.plans.runner.PlanRunnerResult;
import step.planbuilder.BaseArtefacts;
import step.planbuilder.FunctionArtefacts;

public class SequenceHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void test1ChildPassed() {		
		for(ReportNodeStatus status:ReportNodeStatus.values()) {
			if (status!=ReportNodeStatus.RUNNING) {
				test1Child(status);
			}
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
	
	@Test
	public void testDefaultContinueOnErrorDuringSimulation() throws TimeoutException, InterruptedException, IOException {
		// Create a plan with an empty sequence block
		Plan plan = PlanBuilder.create()
				.startBlock(sequence())
					.add(new CheckArtefact(c->{
						c.getExecutionParameters().setMode(ExecutionMode.SIMULATION);
					}))
					.add(echo("'Echo 1'"))
					.add(new CheckArtefact(c->{
						c.getCurrentReportNode().setStatus(ReportNodeStatus.FAILED);
					}))
					.add(echo("'Echo 2'"))
				.endBlock()
				.build();
		
		// Run the plan
		PlanRunner planRunner = new DefaultPlanRunner();
		PlanRunnerResult result = planRunner.run(plan);	
		
		result.waitForExecutionToTerminate();
		
		StringWriter writer = new StringWriter();
		result.printTree(writer);
		
		Assert.assertEquals("Sequence:FAILED:\n" + 
				" CheckArtefact:RUNNING:\n" + 
				" Echo:PASSED:\n" + 
				" CheckArtefact:FAILED:\n" + 
				" Echo:PASSED:\n", writer.toString());	
	}
	
	@Test
	public void testInterrupt() throws Exception {
		// Create a plan with an empty sequence block
		Plan plan = PlanBuilder.create()
				.startBlock(sequence())
					.add(echo("'Echo 1'"))
					.add(new CheckArtefact(c->{
						c.updateStatus(ExecutionStatus.ABORTING);
						c.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
					}))
					.add(echo("'Echo 2'"))
				.endBlock()
				.build();
		
		// Run the plan
		PlanRunner planRunner = new DefaultPlanRunner();
		PlanRunnerResult result = planRunner.run(plan);	
		
		result.waitForExecutionToTerminate();
		
		StringWriter writer = new StringWriter();
		result.printTree(writer);
		
		Assert.assertEquals("Sequence:INTERRUPTED:\n" + 
				" Echo:PASSED:\n" + 
				" CheckArtefact:PASSED:\n", writer.toString());	
	}
	
	@Test
	public void testExceptionInArtefact() throws Exception {
		// Create a plan with an empty sequence block
		Plan plan = PlanBuilder.create()
				.startBlock(sequence())
					.add(new CheckArtefact(c->{
						throw new RuntimeException("My error");
					}))
				.endBlock()
				.build();
		
		// Run the plan
		StringWriter writer = new StringWriter();
		try(PlanRunner planRunner = new DefaultPlanRunner()) {
			PlanRunnerResult result = planRunner.run(plan);
			result.waitForExecutionToTerminate();
			result.printTree(writer);
		}
		
		Assert.assertEquals("Sequence:TECHNICAL_ERROR:\n" + 
				" CheckArtefact:TECHNICAL_ERROR:My error\n", writer.toString());	
	}
	
	@Test
	public void testContinueOnErrorWithinSessions() throws Exception {
		// Create a plan with an empty sequence block
		Plan plan = PlanBuilder.create()
				.startBlock(FunctionArtefacts.session())
					.add(new CheckArtefact(c->{
						throw new RuntimeException("My error");
					}))
					.add(echo("'Echo 1'"))
				.endBlock()
				.build();
		
		// Run the plan
		StringWriter writer = new StringWriter();
		try(PlanRunner planRunner = new DefaultPlanRunner()) {
			PlanRunnerResult result = planRunner.run(plan);
			result.waitForExecutionToTerminate();
			result.printTree(writer);
		}
		
		Assert.assertEquals("Session:TECHNICAL_ERROR:\n" + 
				" CheckArtefact:TECHNICAL_ERROR:My error\n", writer.toString());	
	}
	
	@Test
	public void testEmptySequence() throws Exception {
		Sequence sequence = new Sequence();
	
		// Create a plan with an empty sequence block
		Plan plan = PlanBuilder.create()
				.startBlock(sequence)
				.endBlock()
				.build();
		
		// Run the plan
		PlanRunner planRunner = new DefaultPlanRunner();
		PlanRunnerResult result = planRunner.run(plan);	
		
		result.waitForExecutionToTerminate();
		
		Assert.assertEquals(ReportNodeStatus.PASSED, result.getResult());				
	}
	
	@Test
	public void testBeforeAndAfter() throws Exception {
		// Create a plan with an empty sequence block
		Plan plan = PlanBuilder.create()
				.startBlock(sequence())
				.startBlock(sequence()).withBefore(set("myVar", "'value'")).withAfter(echo("'after sequence'"))
				.add(echo("'in main sequence'"))
				.add(echo("'myVar value is : ' + myVar"))
				.endBlock()
				.endBlock()
				.build();

		// Run the plan
		PlanRunner planRunner = new DefaultPlanRunner();
		PlanRunnerResult result = planRunner.run(plan);

		result.waitForExecutionToTerminate();

		StringWriter writer = new StringWriter();
		result.printTree(writer, true, false);

		Assert.assertEquals("Sequence:PASSED:\n" +
				" Sequence:PASSED:\n" +
				"  [BEFORE]\n" +
				"   Set(myVar = value):PASSED:\n" +
				"  Echo(in main sequence):PASSED:\n" +
				"  Echo(myVar value is : value):PASSED:\n" +
				"  [AFTER]\n" +
				"   Echo(after sequence):PASSED:\n" , writer.toString());
	}

	@Test
	public void testBeforeAndAfterRoot() throws Exception {
		// Create a plan with an empty sequence block
		Plan plan = PlanBuilder.create()
				.startBlock(sequence()).withBefore(echo("'In Before'"), set("myVar", "'value'"))
				.withAfter(echo("'after sequence'"))
				.add(echo("'in main sequence'"))
				.endBlock()
				.build();

		// Run the plan
		PlanRunner planRunner = new DefaultPlanRunner();
		PlanRunnerResult result = planRunner.run(plan);

		result.waitForExecutionToTerminate();

		StringWriter writer = new StringWriter();
		result.printTree(writer, true, false);

		Assert.assertEquals("Sequence:PASSED:\n" +
				" [BEFORE]\n" +
				"  Echo(In Before):PASSED:\n" +
				"  Set(myVar = value):PASSED:\n" +
				" Echo(in main sequence):PASSED:\n" +
				" [AFTER]\n" +
				"  Echo(after sequence):PASSED:\n" , writer.toString());
	}

	@Test
	public void testTechErrorInBefore() throws Exception {
		// Create a plan with an empty sequence block
		Plan plan = PlanBuilder.create()
				.startBlock(sequence()).withBefore(echo("'In Before'"), set("myVar", "test")).withAfter(echo("'after sequence'"))
				.add(echo("'in main sequence'"))
				.endBlock()
				.build();

		// Run the plan
		PlanRunner planRunner = new DefaultPlanRunner();
		PlanRunnerResult result = planRunner.run(plan);

		result.waitForExecutionToTerminate();

		StringWriter writer = new StringWriter();
		result.printTree(writer, true, false);
		Assert.assertEquals("Sequence:TECHNICAL_ERROR:\n" +
				" [BEFORE]\n" +
				"  Echo(In Before):PASSED:\n" +
				"  Set(null = null):TECHNICAL_ERROR:Error while resolving groovy properties in expression: 'test'. The property 'test' could not be found (or accessed). Make sure that the property is defined as variable or parameter and accesible in current scope.. Groovy error: >>> No such property: test for class: Script1 <<<\n" +
				" [AFTER]\n" +
				"  Echo(after sequence):PASSED:\n" , writer.toString());
	}

	@Test
	public void testFailedErrorInBefore() throws Exception {
		// Create a plan with an empty sequence block
		Plan plan = PlanBuilder.create()
				.startBlock(sequence()).withBefore(echo("'In Before'"), check("false")).withAfter(echo("'after sequence'"))
				.add(echo("'in main sequence'"))
				.endBlock()
				.build();
		// Run the plan
		PlanRunner planRunner = new DefaultPlanRunner();
		PlanRunnerResult result = planRunner.run(plan);
		result.waitForExecutionToTerminate();
		//Check the results
		StringWriter writer = new StringWriter();
		result.printTree(writer, true, false);
		Assert.assertEquals("Sequence:FAILED:\n" +
				" [BEFORE]\n" +
				"  Echo(In Before):PASSED:\n" +
				"  Check(false):FAILED:The expression 'false' returned false\n" +
				" [AFTER]\n" +
				"  Echo(after sequence):PASSED:\n" , writer.toString());
	}

	@Test
	public void testTechnicalErrorInChildren() throws Exception {
		// Create a plan with an empty sequence block
		Plan plan = PlanBuilder.create()
				.startBlock(sequence()).withBefore(echo("'In Before'"), set("myVar","'test'")).withAfter(echo("'after sequence'"))
				.add(echo("'in main sequence'"))
				.add(echo("'myVar is ' + myVarNotExists"))
				.endBlock()
				.build();
		// Run the plan
		PlanRunner planRunner = new DefaultPlanRunner();
		PlanRunnerResult result = planRunner.run(plan);
		result.waitForExecutionToTerminate();
		//Check the results
		StringWriter writer = new StringWriter();
		result.printTree(writer, true, false);
		System.out.println(writer.toString());
		Assert.assertEquals("Sequence:TECHNICAL_ERROR:\n" +
				" [BEFORE]\n" +
				"  Echo(In Before):PASSED:\n" +
				"  Set(myVar = test):PASSED:\n" +
				" Echo(in main sequence):PASSED:\n" +
				" Echo:TECHNICAL_ERROR:Error while resolving groovy properties in expression: ''myVar is ' + myVarNotExists'. The property 'myVarNotExists' could not be found (or accessed). Make sure that the property is defined as variable or parameter and accesible in current scope.. Groovy error: >>> No such property: myVarNotExists for class: Script1 <<<\n" +
				" [AFTER]\n" +
				"  Echo(after sequence):PASSED:\n" , writer.toString());
	}

	@Test
	public void testFailedErrorInChildren() throws Exception {
		// Create a plan with an empty sequence block
		Plan plan = PlanBuilder.create()
				.startBlock(sequence()).withBefore(echo("'In Before'"),set("myVar","'test'")).withAfter(echo("'after sequence'"))
				.add(echo("'in main sequence'"))
				.add(check("false"))
				.endBlock()
				.build();
		// Run the plan
		PlanRunner planRunner = new DefaultPlanRunner();
		PlanRunnerResult result = planRunner.run(plan);
		result.waitForExecutionToTerminate();
		//Check the results
		StringWriter writer = new StringWriter();
		result.printTree(writer, true, false);
		System.out.println(writer.toString());
		Assert.assertEquals("Sequence:FAILED:\n" +
				" [BEFORE]\n" +
				"  Echo(In Before):PASSED:\n" +
				"  Set(myVar = test):PASSED:\n" +
				" Echo(in main sequence):PASSED:\n" +
				" Check(false):FAILED:The expression 'false' returned false\n" +
				" [AFTER]\n" +
				"  Echo(after sequence):PASSED:\n" , writer.toString());
	}

	@Test
	public void testFailedErrorInAfter() throws Exception {
		// Create a plan with an empty sequence block
		Plan plan = PlanBuilder.create()
				.startBlock(sequence()).withBefore(echo("'In Before'"), set("myVar","'test'")).withAfter(echo("'after sequence'"), check("false"))
				.add(echo("'in main sequence'"))
				.add(check("myVar == 'test'"))
				.endBlock()
				.build();
		// Run the plan
		PlanRunner planRunner = new DefaultPlanRunner();
		PlanRunnerResult result = planRunner.run(plan);
		result.waitForExecutionToTerminate();
		//Check the results
		StringWriter writer = new StringWriter();
		result.printTree(writer, true, false);
		System.out.println(writer.toString());
		Assert.assertEquals("Sequence:FAILED:\n" +
				" [BEFORE]\n" +
				"  Echo(In Before):PASSED:\n" +
				"  Set(myVar = test):PASSED:\n" +
				" Echo(in main sequence):PASSED:\n" +
				" Check(myVar == 'test'):PASSED:\n" +
				" [AFTER]\n" +
				"  Echo(after sequence):PASSED:\n" +
				"  Check(false):FAILED:The expression 'false' returned false\n" , writer.toString());
	}

	@Test
	public void testBeforeAndAfterSkip() throws Exception {
		// Create a plan with an empty sequence block
		Plan plan = PlanBuilder.create()
				.startBlock(sequence()).add(echo("'in root sequence'"))
				.startBlock(sequence()).withBefore(set("myVar", "'value'")).withAfter(echo("'after sequence'")).withSkip()
				.add(echo("'in main sequence'"))
				.add(echo("'myVar value is : ' + myVar"))
				.endBlock()
				.endBlock()
				.build();

		// Run the plan
		PlanRunner planRunner = new DefaultPlanRunner();
		PlanRunnerResult result = planRunner.run(plan);

		result.waitForExecutionToTerminate();

		StringWriter writer = new StringWriter();
		result.printTree(writer, true, false);


		Assert.assertEquals("Sequence:PASSED:\n" +
				" Echo(in root sequence):PASSED:\n" +
				" Sequence:SKIPPED:\n" , writer.toString());
	}
	
	@Test
	public void testContinueAfterError() throws Exception {
		// Create a plan with an empty sequence block
		Check checkIfContinueAfterError = check("false");
		checkIfContinueAfterError.getContinueParentNodeExecutionOnError().setValue(true);
		Plan plan = PlanBuilder.create()
				.startBlock(sequence())
					.add(check("true"))
					.add(checkIfContinueAfterError)
					.add(check("true"))
				.endBlock()
				.build();
		
		// Run the plan
		PlanRunner planRunner = new DefaultPlanRunner();
		PlanRunnerResult result = planRunner.run(plan);	
		
		result.waitForExecutionToTerminate();
		
		StringWriter writer = new StringWriter();
		result.printTree(writer);
		
		assertEquals("Sequence:FAILED:\n"
				+ " Check:PASSED:\n"
				+ " Check:FAILED:The expression 'false' returned false\n"
				+ " Check:PASSED:\n"
				+ "" , writer.toString());	
	}
}
