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

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import step.artefacts.ForBlock;
import step.artefacts.reports.ForBlockReportNode;
import step.core.artefacts.CheckArtefact;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.DefaultPlanRunner;
import step.datapool.sequence.IntSequenceDataPool;
import step.planbuilder.BaseArtefacts;

public class ForHandlerTest {
	
	@Test
	public void testSuccess() {
		ForBlock f = new ForBlock();
		
		IntSequenceDataPool conf = new IntSequenceDataPool();
		conf.setEnd(new DynamicValue<Integer>(3));;
		conf.setInc(new DynamicValue<Integer>(2));;
		
		f.setDataSource(conf);
		f.setItem(new DynamicValue<String>("item"));
		f.setGlobalCounter(new DynamicValue<String>("globalCounter"));
		f.setUserItem(new DynamicValue<String>("userId"));
		
		AtomicInteger i = new AtomicInteger(1);
		AtomicInteger count = new AtomicInteger(1);
		
		CheckArtefact check1 = new CheckArtefact(context->{
				context.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
				assertEquals(i.get(),(int)context.getVariablesManager().getVariableAsInteger("item"));
				assertEquals(count.get(),(int)context.getVariablesManager().getVariableAsInteger("globalCounter"));
				assertEquals(0,(int)context.getVariablesManager().getVariableAsInteger("userId"));
				i.addAndGet(2);
				count.incrementAndGet();
			});
		
		Plan plan = PlanBuilder.create().startBlock(f).add(check1).endBlock().build();
		DefaultPlanRunner runner = new DefaultPlanRunner();

		runner.run(plan).visitReportNodes(node->{
			Assert.assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		});
	}
	
	@Test
	public void testBreak() {
		ForBlock f = new ForBlock();
		IntSequenceDataPool conf = new IntSequenceDataPool();
		conf.setEnd(new DynamicValue<Integer>(10));;
		
		f.setDataSource(conf);
		
		AtomicInteger i = new AtomicInteger(1);
		
		CheckArtefact check1 = new CheckArtefact(context-> {
				if(i.get()==2) {
					context.getVariablesManager().updateVariable("break", "true");
				}
				i.addAndGet(1);
				context.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
			});
		
		Plan plan = PlanBuilder.create().startBlock(f).add(check1).endBlock().build();
		DefaultPlanRunner runner = new DefaultPlanRunner();

		runner.run(plan).visitReportNodes(node->{
			Assert.assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		});
	}
	
	@Test
	public void testMaxFailedCount() throws IOException {
		ForBlock f = new ForBlock();
		
		IntSequenceDataPool conf = new IntSequenceDataPool();
		conf.setEnd(new DynamicValue<Integer>(10));;
		
		f.setDataSource(conf);
		f.setMaxFailedLoops(new DynamicValue<Integer>(2));
		
		CheckArtefact check1 = new CheckArtefact(context -> {
				context.getCurrentReportNode().setStatus(ReportNodeStatus.FAILED);
			});
		
		Plan plan = PlanBuilder.create().startBlock(f).add(check1).endBlock().build();
		DefaultPlanRunner runner = new DefaultPlanRunner();

		StringWriter writer = new StringWriter();
		runner.run(plan).printTree(writer);
		
		Assert.assertEquals("For:FAILED:\n Iteration 1:FAILED:\n  CheckArtefact:FAILED:\n Iteration 2:FAILED:\n  CheckArtefact:FAILED:\n" ,writer.toString());		
	}
	
	@Test
	public void testTechnicalError() throws IOException, TimeoutException, InterruptedException {
		ForBlock forBlock = new ForBlock();

		IntSequenceDataPool conf = new IntSequenceDataPool();
		conf.setStart(new DynamicValue<Integer>(1));;
		conf.setEnd(new DynamicValue<Integer>(2));;
		
		forBlock.setDataSource(conf);
		
		
		Plan plan = PlanBuilder.create().startBlock(forBlock).add(new CheckArtefact(c -> {
				c.getCurrentReportNode().setStatus(ReportNodeStatus.TECHNICAL_ERROR);
			})).endBlock().build();
		
		DefaultPlanRunner runner = new DefaultPlanRunner();
		
		runner.run(plan).waitForExecutionToTerminate().visitReportTree(e->{
			// Root node
			if(e.getParentNode()==null) {
				Assert.assertEquals(ForBlockReportNode.class, e.getNode().getClass());;
				// Assert that the status of the root node is TECHNICAL_ERROR
				Assert.assertEquals(ReportNodeStatus.TECHNICAL_ERROR, e.getNode().getStatus());
			}
		});
		
	}
	
	@Test
	public void testParallel() throws IOException {
		ForBlock f = new ForBlock();
		
		IntSequenceDataPool conf = new IntSequenceDataPool();
		int iterations = 100;
		conf.setEnd(new DynamicValue<Integer>(iterations));;
		
		f.setDataSource(conf);
		f.setThreads(new DynamicValue<Integer>(2));
		
		ConcurrentHashMap<Integer, AtomicInteger> globalCounter = new ConcurrentHashMap<>();
		ConcurrentHashMap<Integer, AtomicInteger> threadIdMap = new ConcurrentHashMap<>();
		
		CheckArtefact check1 = new CheckArtefact(context->{
				context.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
				threadIdMap.computeIfAbsent(context.getVariablesManager().getVariableAsInteger("userId"), k->new AtomicInteger()).incrementAndGet();
				globalCounter.computeIfAbsent(context.getVariablesManager().getVariableAsInteger("gcounter"), k->new AtomicInteger()).incrementAndGet();
			});
		
		Plan plan = PlanBuilder.create().startBlock(f).add(check1).endBlock().build();
		DefaultPlanRunner runner = new DefaultPlanRunner();

		runner.run(plan).visitReportNodes(node->{
			Assert.assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		}).printTree();
		
		Assert.assertTrue(threadIdMap.get(0).get()>0);
		Assert.assertTrue(threadIdMap.get(1).get()>0);
		
		Assert.assertEquals(iterations, globalCounter.mappingCount());
		Assert.assertEquals(iterations, threadIdMap.get(0).get() + threadIdMap.get(1).get());
	}
	
	@Test
	public void testParallelAuto() throws IOException {
		int iterations = 100;
		ForBlock f = BaseArtefacts.for_(1, iterations);
		
		ConcurrentHashMap<Integer, AtomicInteger> globalCounter = new ConcurrentHashMap<>();
		ConcurrentHashMap<Integer, AtomicInteger> threadIdMap = new ConcurrentHashMap<>();
		
		CheckArtefact check1 = new CheckArtefact(context->{
				context.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
				threadIdMap.computeIfAbsent(context.getVariablesManager().getVariableAsInteger("userId"), k->new AtomicInteger()).incrementAndGet();
				globalCounter.computeIfAbsent(context.getVariablesManager().getVariableAsInteger("gcounter"), k->new AtomicInteger()).incrementAndGet();
			});
		
		Plan plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence())
											.add(BaseArtefacts.set("execution_threads_auto", "2"))
											.startBlock(f)
												.add(check1)
											.endBlock()
										.endBlock()
									.build();
		DefaultPlanRunner runner = new DefaultPlanRunner();

		
		runner.run(plan).visitReportNodes(node->{
			Assert.assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		});
		
		Assert.assertTrue(threadIdMap.get(0).get()>0);
		Assert.assertTrue(threadIdMap.get(1).get()>0);
		
		Assert.assertEquals(iterations, globalCounter.mappingCount());
		Assert.assertEquals(iterations, threadIdMap.get(0).get() + threadIdMap.get(1).get());
	}
	
	@Test
	public void testParallelAutoReentrant() throws IOException {
		int iterations = 10;
		ForBlock f = BaseArtefacts.for_(1, iterations);
		ForBlock for2 = BaseArtefacts.for_(1, iterations);
		// the following should be overridden by the ThreadPool as the second for is in a reentrant parallelism 
		for2.setThreads(new DynamicValue<Integer>(2));
		
		ConcurrentHashMap<Integer, AtomicInteger> globalCounter = new ConcurrentHashMap<>();
		ConcurrentHashMap<Integer, AtomicInteger> threadIdMap = new ConcurrentHashMap<>();
		
		CheckArtefact check1 = new CheckArtefact(context->{
				context.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
				threadIdMap.computeIfAbsent(context.getVariablesManager().getVariableAsInteger("userId"), k->new AtomicInteger()).incrementAndGet();
				globalCounter.computeIfAbsent(context.getVariablesManager().getVariableAsInteger("gcounter"), k->new AtomicInteger()).incrementAndGet();
			});
		
		Plan plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence())
											.add(BaseArtefacts.set("execution_threads_auto", "2"))
											.startBlock(f)
												.startBlock(for2)
													.add(check1)
												.endBlock()
											.endBlock()
										.endBlock()
									.build();
		DefaultPlanRunner runner = new DefaultPlanRunner();

		
		runner.run(plan).visitReportNodes(node->{
			Assert.assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		});
		
		globalCounter.forEach((k,v)->Assert.assertEquals(iterations, v.get()));
		Assert.assertEquals(iterations*iterations, threadIdMap.get(0).get());
	}
}

