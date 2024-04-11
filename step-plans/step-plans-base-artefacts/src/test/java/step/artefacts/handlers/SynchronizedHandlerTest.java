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

import static step.planbuilder.BaseArtefacts.synchronized_;
import static step.planbuilder.BaseArtefacts.testScenario;
import static step.planbuilder.BaseArtefacts.threadGroup;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import junit.framework.Assert;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.Synchronized;
import step.artefacts.TestScenario;
import step.artefacts.ThreadGroup;
import step.artefacts.handlers.functions.TokenAutoscalingExecutionPlugin;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.execution.ExecutionEngineContext;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.threadpool.ThreadPoolPlugin;

public class SynchronizedHandlerTest {
	
	private ExecutionEngine engine = ExecutionEngine.builder().withPlugin(new ThreadPoolPlugin()).withPlugin(new BaseArtefactPlugin()).withPlugin(new AbstractExecutionEnginePlugin() {
		@Override
		public void initializeExecutionContext(ExecutionEngineContext executionEngineContext,
				ExecutionContext executionContext) {
			executionContext.getArtefactHandlerRegistry().put(TestArtefact.class, TestArtefactHandler.class);
		}
	}).withPlugin(new TokenAutoscalingExecutionPlugin()).build();
	
	@Test
	public void testUnnamedLocalLock() throws IOException {		
		AtomicInteger maxParallelism = new AtomicInteger(0);
		AtomicInteger parallelism = new AtomicInteger(0);
		AtomicInteger iterations = new AtomicInteger(0);
		
		Plan plan = PlanBuilder.create().startBlock(testScenario())
											.startBlock(threadGroup(5,10))
												.startBlock(synchronized_("", false))
													.add(testArtefact(maxParallelism, parallelism, iterations))
												.endBlock()
											.endBlock()
											.startBlock(threadGroup(5,10))
												.startBlock(synchronized_("", false))
													.add(testArtefact(maxParallelism, parallelism, iterations))
												.endBlock()
											.endBlock()
										.endBlock()
									.build();
		
		engine.execute(plan).visitReportNodes(node->{
			Assert.assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		});
		// Unnamed lock are equivalent to a lock using the artefactID as lock name and is therefore local to a specific artefact
		// we're therefore expecting a parallelism of 2 in this test case
		Assert.assertEquals(2, maxParallelism.get());
		Assert.assertEquals(100, iterations.get());
	}
	
	@Test
	public void testNamedLocalLock() throws IOException {		
		AtomicInteger maxParallelism = new AtomicInteger(0);
		AtomicInteger parallelism = new AtomicInteger(0);
		AtomicInteger iterations = new AtomicInteger(0);
		
		Plan plan = PlanBuilder.create().startBlock(testScenario())
											.startBlock(threadGroup(5,10))
												.startBlock(synchronized_("Lock1", false))
													.add(testArtefact(maxParallelism, parallelism, iterations))
												.endBlock()
											.endBlock()
											.startBlock(threadGroup(5,10))
												.startBlock(synchronized_("Lock2", false))
													.add(testArtefact(maxParallelism, parallelism, iterations))
												.endBlock()
											.endBlock()
											.startBlock(threadGroup(5,10))
												.startBlock(synchronized_("Lock2", false))
													.add(testArtefact(maxParallelism, parallelism, iterations))
												.endBlock()
											.endBlock()
										.endBlock()
									.build();
		
		engine.execute(plan).visitReportNodes(node->{
			Assert.assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		});
		Assert.assertEquals(2, maxParallelism.get());
		Assert.assertEquals(150, iterations.get());
	}
	
	@Test
	public void testNamedGlobalLock() throws IOException, InterruptedException {		
		AtomicInteger maxParallelism = new AtomicInteger(0);
		AtomicInteger parallelism = new AtomicInteger(0);
		AtomicInteger iterations = new AtomicInteger(0);
		
		Plan plan1 = PlanBuilder.create().startBlock(testScenario())
											.startBlock(threadGroup(5,10))
												.startBlock(synchronized_("Lock1", true))
													.add(testArtefact(maxParallelism, parallelism, iterations))
												.endBlock()
											.endBlock()
										.endBlock()
									.build();

		Plan plan2 = PlanBuilder.create().startBlock(testScenario())
				.startBlock(threadGroup(5,10))
					.startBlock(synchronized_("Lock1", true))
						.add(testArtefact(maxParallelism, parallelism, iterations))
					.endBlock()
				.endBlock()
			.endBlock()
		.build();
		
		ExecutorService threadPool = Executors.newFixedThreadPool(2);
		threadPool.execute(()->engine.execute(plan1));
		threadPool.execute(()->engine.execute(plan2));

		threadPool.shutdown();
		threadPool.awaitTermination(10, TimeUnit.SECONDS);
		
		Assert.assertEquals(1, maxParallelism.get());
		Assert.assertEquals(100, iterations.get());
	}

	protected TestArtefact testArtefact(AtomicInteger maxParallelism, AtomicInteger parallelism,
			AtomicInteger iterations) {
		return new TestArtefact(iterations, parallelism, maxParallelism);
	}
	
	@Test
	public void testNamedLocalGlobal() throws IOException {		
		Synchronized synchronized1 = new Synchronized();
		synchronized1.setGlobalLock(new DynamicValue<Boolean>(false));
		synchronized1.setLockName(new DynamicValue<String>("MyLock"));
		
		Synchronized synchronized2 = new Synchronized();
		synchronized1.setGlobalLock(new DynamicValue<Boolean>(false));
		synchronized1.setLockName(new DynamicValue<String>("MyLock2"));

		TestScenario testScenario = new TestScenario();
		
		ThreadGroup threadGroup = new ThreadGroup();
		threadGroup.setIterations(new DynamicValue<Integer>(10));
		threadGroup.setUsers(new DynamicValue<Integer>(5));
		
		ThreadGroup threadGroup2 = new ThreadGroup();
		threadGroup2.setIterations(new DynamicValue<Integer>(10));
		threadGroup2.setUsers(new DynamicValue<Integer>(5));
		
		AtomicInteger maxParallelism = new AtomicInteger(0);
		AtomicInteger parallelism = new AtomicInteger(0);
		AtomicInteger iterations = new AtomicInteger(0);
		TestArtefact c = testArtefact(maxParallelism, parallelism, iterations);
		
		Plan plan = PlanBuilder.create().startBlock(testScenario)
											.startBlock(threadGroup)
												.startBlock(synchronized1)
													.add(c)
												.endBlock()
											.endBlock()
											.startBlock(threadGroup2)
												.startBlock(synchronized2)
													.add(c)
												.endBlock()
											.endBlock()
										.endBlock()
									.build();
		
		engine.execute(plan).visitReportNodes(node->{
			Assert.assertEquals(ReportNodeStatus.PASSED, node.getStatus());
		});
		Assert.assertEquals(2, maxParallelism.get());
		Assert.assertEquals(100, iterations.get());
	}
	
	@Artefact(test = true)
	public static class TestArtefact extends AbstractArtefact {
		
		private AtomicInteger iterations;
		private AtomicInteger parallelism;
		private AtomicInteger maxParallelism;
		
		public TestArtefact() {
			super();
		}

		public TestArtefact(AtomicInteger iterations, AtomicInteger parallelism, AtomicInteger maxParallelism) {
			super();
			this.iterations = iterations;
			this.parallelism = parallelism;
			this.maxParallelism = maxParallelism;
		}

		public AtomicInteger getIterations() {
			return iterations;
		}

		public void setIterations(AtomicInteger iterations) {
			this.iterations = iterations;
		}

		public AtomicInteger getParallelism() {
			return parallelism;
		}

		public void setParallelism(AtomicInteger parallelism) {
			this.parallelism = parallelism;
		}

		public AtomicInteger getMaxParallelism() {
			return maxParallelism;
		}

		public void setMaxParallelism(AtomicInteger maxParallelism) {
			this.maxParallelism = maxParallelism;
		}
	};
	
	public static class TestArtefactHandler extends ArtefactHandler<TestArtefact, ReportNode> {

		@Override
		protected void createReportSkeleton_(ReportNode parentNode, TestArtefact testArtefact) {
		}

		@Override
		protected void execute_(ReportNode node, TestArtefact testArtefact) throws InterruptedException {
			int currentParallelism = testArtefact.parallelism.incrementAndGet();
			if(currentParallelism>testArtefact.maxParallelism.get()) {
				testArtefact.maxParallelism.set(currentParallelism);
			}
			Thread.sleep(10);
			testArtefact.iterations.incrementAndGet();
			testArtefact.parallelism.decrementAndGet();
			node.setStatus(ReportNodeStatus.PASSED);
		}

		@Override
		public ReportNode createReportNode_(ReportNode parentNode,
				TestArtefact testArtefact) {
			return new ReportNode();
		}

	}

}
