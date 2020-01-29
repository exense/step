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

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import junit.framework.Assert;
import step.artefacts.Check;
import step.artefacts.CheckArtefact;
import step.artefacts.Sleep;
import step.artefacts.ThreadGroup;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.DefaultPlanRunner;
import step.planbuilder.BaseArtefacts;

public class ThreadGroupHandlerTest extends AbstractArtefactHandlerTest {
	
	@Test
	public void testStatusReportingFailed() throws Exception {
		ThreadGroup artefact = new ThreadGroup();
		
		Plan plan = PlanBuilder.create().startBlock(artefact).add(passedCheck()).add(failedCheck()).endBlock().build();
		DefaultPlanRunner runner = new DefaultPlanRunner();
		
		StringWriter writer = new StringWriter();
		runner.run(plan).printTree(writer);
		
		Assert.assertTrue(writer.toString().startsWith("ThreadGroup:"+ReportNodeStatus.FAILED));
	}
	
	@Test
	public void testStatusReportingPassed() throws Exception {
		ThreadGroup artefact = new ThreadGroup();
		artefact.getIterations().setValue(3);
		
		Plan plan = PlanBuilder.create().startBlock(artefact).add(passedCheck()).add(passedCheck()).endBlock().build();
		DefaultPlanRunner runner = new DefaultPlanRunner();
		
		StringWriter writer = new StringWriter();
		runner.run(plan).printTree(writer);
		
		Assert.assertEquals("ThreadGroup:PASSED:\n" + 
				" Group_1_Iteration_1:PASSED:\n" + 
				"  Check:PASSED:\n" + 
				"  Check:PASSED:\n" + 
				" Group_1_Iteration_2:PASSED:\n" + 
				"  Check:PASSED:\n" + 
				"  Check:PASSED:\n" + 
				" Group_1_Iteration_3:PASSED:\n" + 
				"  Check:PASSED:\n" + 
				"  Check:PASSED:\n" + 
				"", writer.toString());
	}
	
	@Test
	public void testStatusReportingError() throws Exception {
		Plan plan = PlanBuilder.create().startBlock(new ThreadGroup()).add(passedCheck()).add(errorCheck()).add(passedCheck()).endBlock().build();
		DefaultPlanRunner runner = new DefaultPlanRunner();
		
		StringWriter writer = new StringWriter();
		runner.run(plan).printTree(writer);
		
		Assert.assertTrue(writer.toString().startsWith("ThreadGroup:"+ReportNodeStatus.TECHNICAL_ERROR));
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
		
		Assert.assertTrue(writer.toString().startsWith("ThreadGroup:"+ReportNodeStatus.PASSED));
		Assert.assertTrue(count.get()<10);
	}
	
	@Test
	public void testMaxDurationDefault() throws Exception {
		AtomicInteger count = new AtomicInteger(0);

		StringWriter writer = testMaxDuration(0, 1000, count);
		
		Assert.assertTrue(writer.toString().startsWith("ThreadGroup:"+ReportNodeStatus.PASSED));
		Assert.assertEquals(10, count.get());
	}


	public StringWriter testMaxDuration(long sleepTime, long maxDuration, AtomicInteger count) throws IOException {
		ThreadGroup artefact = new ThreadGroup();
		artefact.setMaxDuration(new DynamicValue<Integer>(100));
		artefact.setIterations(new DynamicValue<Integer>(10));
		
		Sleep sleep = new Sleep();
		sleep.setDuration(new DynamicValue<Long>(sleepTime));
		
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

