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
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import junit.framework.Assert;
import step.artefacts.ThreadGroup;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.ContainsDynamicValues;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.plans.runner.DefaultPlanRunner;
import step.planbuilder.PlanBuilder;

public class TestGroupHandler {

	@Test
	public void test() throws IOException {		
		ThreadGroup threadGroup = new ThreadGroup();
		threadGroup.setIterations(new DynamicValue<Integer>(10));
		threadGroup.setPacing(new DynamicValue<Integer>(10));
		threadGroup.setUsers(new DynamicValue<Integer>(5));
		
		
		AtomicInteger iterations = new AtomicInteger(0);
		TestArtefact c = new TestArtefact(iterations);
		
		Plan plan = PlanBuilder.create().startBlock(threadGroup).add(c).endBlock().build();
		
		DefaultPlanRunner runner = new DefaultPlanRunner();
		
		long t1 = System.currentTimeMillis();
		runner.run(plan).visitReportTree(node->{
			Assert.assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		});
		long t2 = System.currentTimeMillis();
		
		Assert.assertEquals(50, iterations.get());
		
		// This ensures that the artefact instance is properly cloned and 
		// not shared between the threads
		Assert.assertEquals(0, (int)c.localCount.get());
		Assert.assertEquals(0, (int)c.member.localCount.get());
		Assert.assertTrue((t2-t1)>100);
		
	}
	
	@Artefact(handler=TestArtefactHandler.class)
	public static class TestArtefact extends AbstractArtefact {
		
		AtomicInteger iterations;
		
		DynamicValue<Integer> localCount = new DynamicValue<Integer>(0);
		
		TestArtefactMember member = new TestArtefactMember();
		
		public TestArtefact() {
			super();
		}

		public TestArtefact(AtomicInteger iterations) {
			this.iterations = iterations;
		}

		public AtomicInteger getIterations() {
			return iterations;
		}

		public void setIterations(AtomicInteger iterations) {
			this.iterations = iterations;
		}

		public DynamicValue<Integer> getLocalCount() {
			return localCount;
		}

		public void setLocalCount(DynamicValue<Integer> localCount) {
			this.localCount = localCount;
		}

		@ContainsDynamicValues
		public TestArtefactMember getMember() {
			return member;
		}

		public void setMember(TestArtefactMember member) {
			this.member = member;
		}
	};
	
	public static class TestArtefactMember {
		
		DynamicValue<Integer> localCount = new DynamicValue<Integer>(0);

	}
	
	public static class TestArtefactHandler extends ArtefactHandler<TestArtefact, ReportNode> {

		@Override
		protected void createReportSkeleton_(ReportNode parentNode, TestArtefact testArtefact) {
		}

		@Override
		protected void execute_(ReportNode node, TestArtefact testArtefact) {
			node.setStatus(ReportNodeStatus.PASSED);
			testArtefact.iterations.incrementAndGet();
			testArtefact.localCount.setValue(testArtefact.localCount.get()+1);	
			testArtefact.member.localCount.setValue(testArtefact.member.localCount.get()+1);
		}

		@Override
		public ReportNode createReportNode_(ReportNode parentNode,
				TestArtefact testArtefact) {
			return new ReportNode();
		}

	}
}

