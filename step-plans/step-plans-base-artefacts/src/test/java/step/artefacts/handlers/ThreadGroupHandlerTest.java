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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static step.planbuilder.BaseArtefacts.afterSequence;
import static step.planbuilder.BaseArtefacts.afterThread;
import static step.planbuilder.BaseArtefacts.beforeSequence;
import static step.planbuilder.BaseArtefacts.beforeThread;
import static step.planbuilder.BaseArtefacts.check;
import static step.planbuilder.BaseArtefacts.echo;
import static step.planbuilder.BaseArtefacts.runnable;
import static step.planbuilder.BaseArtefacts.threadGroup;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import step.artefacts.Check;
import step.artefacts.Sleep;
import step.artefacts.ThreadGroup;
import step.core.artefacts.CheckArtefact;
import step.core.artefacts.reports.ReportNodeStatus;
import ch.exense.commons.core.model.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;

public class ThreadGroupHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void testStatusReportingFailed() throws Exception {
		ThreadGroup artefact = new ThreadGroup();
		
		Plan plan = PlanBuilder.create().startBlock(artefact).add(passedCheck()).add(failedCheck()).endBlock().build();
		
		StringWriter writer = new StringWriter();
		executionEngine.execute(plan).printTree(writer);
		
		assertTrue(writer.toString().startsWith("ThreadGroup:"+ReportNodeStatus.FAILED));
	}
	
	@Test
	public void testStatusReportingPassed() throws Exception {
		ThreadGroup artefact = new ThreadGroup();
		artefact.getIterations().setValue(3);
		
		Plan plan = PlanBuilder.create().startBlock(artefact).add(passedCheck()).add(passedCheck()).endBlock().build();
		
		StringWriter writer = new StringWriter();
		executionEngine.execute(plan).printTree(writer);
		
		assertEquals("ThreadGroup:PASSED:\n" + 
				" Thread 1:PASSED:\n" + 
				"  Session:PASSED:\n" + 
				"   Iteration 1:PASSED:\n" + 
				"    Check:PASSED:\n" + 
				"    Check:PASSED:\n" + 
				"   Iteration 2:PASSED:\n" + 
				"    Check:PASSED:\n" + 
				"    Check:PASSED:\n" + 
				"   Iteration 3:PASSED:\n" + 
				"    Check:PASSED:\n" + 
				"    Check:PASSED:\n", writer.toString());
	}
	
	@Test
	public void testStatusReportingError() throws Exception {
		Plan plan = PlanBuilder.create().startBlock(new ThreadGroup()).add(passedCheck()).add(errorCheck()).add(passedCheck()).endBlock().build();
		
		StringWriter writer = new StringWriter();
		executionEngine.execute(plan).printTree(writer);
		
		assertTrue(writer.toString().startsWith("ThreadGroup:"+ReportNodeStatus.TECHNICAL_ERROR));
	}

	private Check passedCheck() {
		Check passedCheck = new Check();
		passedCheck.setExpression(new DynamicValue<Boolean>(true));
		return passedCheck;
	}
	
	private Check failedCheck() {
		Check failedCheck = new Check();
		failedCheck.setExpression(new DynamicValue<Boolean>(false));
		return failedCheck;
	}
	
	private Check errorCheck() {
		Check errorCheck = new Check();
		return errorCheck;
	}
	
	@Test
	public void testMaxDurationExceeded() throws Exception {
		AtomicInteger count = new AtomicInteger(0);

		StringWriter writer = testMaxDuration(50, 100, count);
		
		assertTrue(writer.toString().startsWith("ThreadGroup:"+ReportNodeStatus.PASSED));
		assertTrue(count.get()<10);
	}
	
	@Test
	public void testMaxDurationDefault() throws Exception {
		AtomicInteger count = new AtomicInteger(0);

		StringWriter writer = testMaxDuration(0, 1000, count);
		
		assertTrue(writer.toString().startsWith("ThreadGroup:"+ReportNodeStatus.PASSED));
		assertEquals(10, count.get());
	}
	
	@Test
	public void testMaxDurationWithoutMaxIterationCountDefault() throws Exception {
		AtomicInteger count = new AtomicInteger(0);

		StringWriter writer = testMaxDuration(50, 100, 0, count);
		
		assertTrue(writer.toString().startsWith("ThreadGroup:"+ReportNodeStatus.PASSED));
		assertTrue(count.get()<10);
	}

	@Test
	public void testPacing() throws IOException, TimeoutException, InterruptedException {
		int nIterations = 50;
		int pacingMs = 100;

		ThreadGroup artefact = new ThreadGroup();
		artefact.setPacing(new DynamicValue<Integer>(pacingMs));
		artefact.setIterations(new DynamicValue<Integer>(nIterations));

		long t1 = System.currentTimeMillis();
		AtomicInteger count = new AtomicInteger();
		CheckArtefact check = new CheckArtefact(c-> {
			count.incrementAndGet();
			c.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
		});
		
		Plan plan = PlanBuilder.create().startBlock(artefact).add(check).endBlock().build();
		
		ReportNodeStatus result = executionEngine.execute(plan).waitForExecutionToTerminate().getResult();
		assertEquals(ReportNodeStatus.PASSED, result);
		assertEquals(nIterations, count.get());
		long duration = System.currentTimeMillis()-t1;
		// Assert that the duration is higher than pacing x number of iterations
		// and thus that the pacing has been taken into account
		assertTrue(duration>=pacingMs*nIterations);
	}

	public StringWriter testMaxDuration(long sleepTime, int maxDuration, AtomicInteger count) throws IOException {
		return testMaxDuration(sleepTime, maxDuration, 10, count);
	}
	
	public StringWriter testMaxDuration(long sleepTime, int maxDuration, int maxIterations, AtomicInteger count) throws IOException {
		ThreadGroup artefact = new ThreadGroup();
		artefact.setMaxDuration(new DynamicValue<Integer>(maxDuration));
		artefact.setIterations(new DynamicValue<Integer>(maxIterations));
		
		Sleep sleep = new Sleep();
		sleep.setDuration(new DynamicValue<Long>(sleepTime));
		
		CheckArtefact check = new CheckArtefact(c-> {
			count.incrementAndGet();
		});
		
		Plan plan = PlanBuilder.create().startBlock(artefact).add(sleep).add(check).endBlock().build();
		
		StringWriter writer = new StringWriter();
		executionEngine.execute(plan).printTree(writer);
		return writer;
	}
	
	@Test
	public void testBeforeAndAfterThread() throws Exception {
		final AtomicInteger globalCounterActual = new AtomicInteger();
		final List<String> userIds = new ArrayList<>();
		// Create a plan with an empty sequence block
		Plan plan = PlanBuilder.create()
				.startBlock(threadGroup(1, 2))
					.startBlock(beforeThread())
						// the userId should be available in the beforeThread
						.add(echo("'Before...'+userId"))
					.endBlock()
					// the variables userId, literationId, gcounter should be available in the beforeThread
					.add(echo("'Iteration'+userId+literationId+gcounter"))
					.add(runnable(c->globalCounterActual.addAndGet(c.getVariablesManager().getVariableAsInteger("gcounter"))))
					.add(runnable(c->userIds.add(c.getVariablesManager().getVariableAsString("userId"))))
					.startBlock(afterThread())
						.add(echo("'After...'+userId"))
					.endBlock()
				.endBlock()
				.build();
		
		// Run the plan
		PlanRunnerResult result = executionEngine.execute(plan);	
		
		result.waitForExecutionToTerminate();
		
		StringWriter writer = new StringWriter();
		result.printTree(writer);
		
		assertEquals("ThreadGroup:PASSED:\n" + 
				" Thread 1:PASSED:\n" + 
				"  Session:PASSED:\n" + 
				"   BeforeThread:PASSED:\n" + 
				"    Echo:PASSED:\n" + 
				"   Iteration 1:PASSED:\n" + 
				"    Echo:PASSED:\n" + 
				"    CheckArtefact:RUNNING:\n" + 
				"    CheckArtefact:RUNNING:\n" + 
				"   Iteration 2:PASSED:\n" + 
				"    Echo:PASSED:\n" + 
				"    CheckArtefact:RUNNING:\n" + 
				"    CheckArtefact:RUNNING:\n" + 
				"   AfterThread:PASSED:\n" + 
				"    Echo:PASSED:\n" + 
				"" , writer.toString());	
		
		assertEquals(3, globalCounterActual.get());
		assertArrayEquals(new String[] {"1","1"}, userIds.toArray());
	}
	
	@Test
	public void testBeforeAndAfterThreadCombinedWithBeforeAndAfterSequence() throws Exception {
		// Create a plan with an empty sequence block
		Plan plan = PlanBuilder.create()
				.startBlock(threadGroup(1, 2))
					.startBlock(beforeThread())
						.add(echo("'Before...'+userId"))
					.endBlock()
					.startBlock(beforeSequence())
						// literationId should be available in BeforeSequence 
						.add(echo("'Before...'+literationId"))
					.endBlock()
					.add(echo("'Iteration'"))
					.add(check("false"))
					.startBlock(afterSequence())
						.add(echo("'After...'"))
					.endBlock()
					.startBlock(afterThread())
						.add(echo("'After...'"))
					.endBlock()
				.endBlock()
				.build();
		
		// Run the plan
		PlanRunnerResult result = executionEngine.execute(plan);	
		
		result.waitForExecutionToTerminate();
		
		StringWriter writer = new StringWriter();
		result.printTree(writer);
		
		assertEquals("ThreadGroup:FAILED:\n" + 
				" Thread 1:FAILED:\n" + 
				"  Session:FAILED:\n" + 
				"   BeforeThread:PASSED:\n" + 
				"    Echo:PASSED:\n" + 
				"   Iteration 1:FAILED:\n" + 
				"    BeforeSequence:PASSED:\n" + 
				"     Echo:PASSED:\n" + 
				"    Echo:PASSED:\n" + 
				"    Check:FAILED:\n" + 
				"    AfterSequence:PASSED:\n" + 
				"     Echo:PASSED:\n" + 
				"   Iteration 2:FAILED:\n" + 
				"    BeforeSequence:PASSED:\n" + 
				"     Echo:PASSED:\n" + 
				"    Echo:PASSED:\n" + 
				"    Check:FAILED:\n" + 
				"    AfterSequence:PASSED:\n" + 
				"     Echo:PASSED:\n" + 
				"   AfterThread:PASSED:\n" + 
				"    Echo:PASSED:\n" , writer.toString());	
	}
}

