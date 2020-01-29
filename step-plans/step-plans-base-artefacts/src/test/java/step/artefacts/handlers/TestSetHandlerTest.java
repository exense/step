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
import java.util.HashSet;

import org.junit.Test;

import junit.framework.Assert;
import step.artefacts.Check;
import step.artefacts.CheckArtefact;
import step.artefacts.TestSet;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.DefaultPlanRunner;

public class TestSetHandlerTest extends AbstractArtefactHandlerTest {
	
	//@Test
	public void testParallel() throws Exception {
		HashSet<Long> threadIdSet = new HashSet<>();

		execute(threadIdSet, "10", "DummyName{childID}");
		
		assertEquals("Ensure that the TestSetHandler runs the child artefacts in parallel", 10, threadIdSet.size());
	}
	
	//@Test
	public void testSequential() throws Exception {
		HashSet<Long> threadIdSet = new HashSet<>();

		execute(threadIdSet, "1", "DummyName{childID}");
		
		assertEquals("Ensure that the TestSetHandler runs the child artefacts sequentially", 1, threadIdSet.size());
	}
	
	@Test
	public void testStatusReportingFailed() throws Exception {
		Plan plan = PlanBuilder.create().startBlock(new TestSet()).add(passedCheck()).add(failedCheck()).endBlock().build();
		DefaultPlanRunner runner = new DefaultPlanRunner();
		
		StringWriter writer = new StringWriter();
		runner.run(plan).printTree(writer);
		
		Assert.assertTrue(writer.toString().startsWith("TestSet:"+ReportNodeStatus.FAILED));
	}
	
	@Test
	public void testStatusReportingPassed() throws Exception {
		Plan plan = PlanBuilder.create().startBlock(new TestSet()).add(passedCheck()).add(passedCheck()).endBlock().build();
		DefaultPlanRunner runner = new DefaultPlanRunner();
		
		StringWriter writer = new StringWriter();
		runner.run(plan).printTree(writer);
		
		Assert.assertTrue(writer.toString().startsWith("TestSet:"+ReportNodeStatus.PASSED));
	}
	
	@Test
	public void testStatusReportingError() throws Exception {
		Plan plan = PlanBuilder.create().startBlock(new TestSet()).add(passedCheck()).add(errorCheck()).add(passedCheck()).endBlock().build();
		DefaultPlanRunner runner = new DefaultPlanRunner();
		
		StringWriter writer = new StringWriter();
		runner.run(plan).printTree(writer);
		
		Assert.assertTrue(writer.toString().startsWith("TestSet:"+ReportNodeStatus.TECHNICAL_ERROR));
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

	private void execute(HashSet<Long> threadIdSet, String tecExecutionThreads, String childNamePattern) {
		setupContext();
		
		context.getVariablesManager().putVariable(
				context.getReport(), "tec.execution.threads", tecExecutionThreads);
		
		context.getVariablesManager().getVariable("var");
		
		TestSet set = new TestSet();
		
		int nChilds = 20;
		
		for(int j=0;j<nChilds;j++) {
			set.addChild(new CheckArtefact(c-> {
					synchronized (threadIdSet) {
						threadIdSet.add(Thread.currentThread().getId());						
					}
					context.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
				}));
		}
		
		createSkeleton(set);
		execute(set);
		
		ReportNode child = getFirstReportNode();
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);
		
		assertEquals(nChilds, getChildren(child).size());
		
		for(ReportNode node:getChildren(child)) {
			assertEquals(node.getStatus(), ReportNodeStatus.PASSED);					
		}
	}
}

