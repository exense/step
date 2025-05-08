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

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import step.threadpool.ThreadPool;

public class ForHandlerTest {

	private static final Logger logger = LoggerFactory.getLogger(ForHandlerTest.class);

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

		logger.info("threadIdMap content: " + threadIdMap);

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
											.add(BaseArtefacts.set(ThreadPool.EXECUTION_THREADS_AUTO, "2"))
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
        int outerThreads = 2;
        int innerThreads = 2;
        Integer[] auto_threads = new Integer[]{null, 5, 11};
        Integer[] inner_Iterations = new Integer[]{2, 6, 10};
        Integer[] outer_Iterations = new Integer[]{2, 6, 10};
        for (Integer execution_threads_auto : auto_threads) {
            for (int outerIterations : outer_Iterations) {
                for (int innerIterations : inner_Iterations) {
                    //System.err.printf("outer=%d inner=%d auto=%s%n", outerIterations, innerIterations, execution_threads_auto);

                    ForBlock for1 = BaseArtefacts.for_(1, outerIterations);
                    ForBlock for2 = BaseArtefacts.for_(1, innerIterations);
                    // this one should be overridden by the ThreadPool to the value of the execution_threads_auto parameter (if present)
                    for1.setThreads(new DynamicValue<>(outerThreads));
                    // this one should be overridden by the ThreadPool to be 1 as the second for is in a reentrant parallelism (if execution_threads_auto is present)
                    for2.setThreads(new DynamicValue<>(innerThreads));

                    ConcurrentHashMap<Integer, AtomicInteger> innerCounter = new ConcurrentHashMap<>();
                    ConcurrentHashMap<Integer, AtomicInteger> innerThreadIdMap = new ConcurrentHashMap<>();
                    ConcurrentHashMap<Integer, AtomicInteger> outerThreadIdMap = new ConcurrentHashMap<>();

                    CheckArtefact checkOuter = new CheckArtefact(context -> {
                        context.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
                        outerThreadIdMap.computeIfAbsent(context.getVariablesManager().getVariableAsInteger("userId"), k -> new AtomicInteger()).incrementAndGet();
                        //System.err.println("outer " + Thread.currentThread().getName() + " userId=" + context.getVariablesManager().getVariableAsInteger("userId") + " gcounter=" + context.getVariablesManager().getVariableAsInteger("gcounter"));
                    });

                    CheckArtefact checkInner = new CheckArtefact(context -> {
                        // pretend that some work is actually taking place, otherwise we might be finishing TOO fast
                        // so that not all outer threads are even needed (which could break the test assertions)
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        context.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);
                        innerThreadIdMap.computeIfAbsent(context.getVariablesManager().getVariableAsInteger("userId"), k -> new AtomicInteger()).incrementAndGet();
                        innerCounter.computeIfAbsent(context.getVariablesManager().getVariableAsInteger("gcounter"), k -> new AtomicInteger()).incrementAndGet();
                        //System.err.println("inner " + Thread.currentThread().getName() + " userId=" + context.getVariablesManager().getVariableAsInteger("userId") + " gcounter=" + context.getVariablesManager().getVariableAsInteger("gcounter"));
                    });

                    String variableName = execution_threads_auto != null ? ThreadPool.EXECUTION_THREADS_AUTO : "ignored";
                    Plan plan = PlanBuilder.create().startBlock(BaseArtefacts.sequence())
                            .add(BaseArtefacts.set(variableName, "" + execution_threads_auto))
                            .startBlock(for1)
                                .add(checkOuter)
                                    .startBlock(for2)
                                        .add(checkInner)
                                    .endBlock()
                                .endBlock()
                            .endBlock()
                            .build();
                    DefaultPlanRunner runner = new DefaultPlanRunner();


                    runner.run(plan).visitReportNodes(node -> {
                        Assert.assertEquals(ReportNodeStatus.PASSED, node.getStatus());
                    });

                    // OUTER LOOP checks:
                    // check number of outer iterations performed
                    Assert.assertEquals(outerIterations, outerThreadIdMap.values().stream().mapToInt(AtomicInteger::get).sum());
                    if (execution_threads_auto != null) {
                        // check that `execution_threads_auto` threads have been used (or possibly fewer if the number of iterations is lower)
                        Assert.assertEquals(Math.min(execution_threads_auto, outerIterations), outerThreadIdMap.mappingCount());
                    } else {
                        // check that `outerThreads` threads have been used.
                        Assert.assertEquals(outerThreads, outerThreadIdMap.mappingCount());
                    }

                    // INNER LOOP checks:
                    // total iterations == outerIterations * innerIterations
                    Assert.assertEquals(outerIterations * innerIterations, innerThreadIdMap.values().stream().mapToInt(AtomicInteger::get).sum());
                    if (execution_threads_auto != null) {
                        // all iterations should have happened on the same (logical) threadId
                        Assert.assertEquals(1, innerThreadIdMap.mappingCount());
                    } else {
                        Assert.assertEquals(innerThreads, innerThreadIdMap.mappingCount());
                    }
                    // the innerCounter should have `innerIterations` keys, each having the value `outerIterations`,
                    // meaning: each (inner) gcounter value (1 <= innerIterations) was encountered `outerIterations` times.
                    Assert.assertEquals(innerIterations, innerCounter.mappingCount());
                    innerCounter.forEach((k, v) -> Assert.assertEquals(outerIterations, v.get()));
                }
            }

        }
	}
}

