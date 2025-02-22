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

import com.fasterxml.jackson.annotation.JsonIgnore;
import step.artefacts.*;
import step.artefacts.ThreadGroup;
import step.artefacts.handlers.functions.MultiplyingTokenForecastingContext;
import step.artefacts.handlers.functions.TokenForecastingContext;
import step.artefacts.handlers.loadtesting.Pacer;
import step.artefacts.reports.ThreadReportNode;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.artefacts.ChildrenBlock;
import step.core.artefacts.handlers.*;
import step.core.artefacts.reports.ParentSource;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.resolvedplan.ResolvedChildren;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.PlanAccessor;
import step.functions.accessor.FunctionAccessor;
import step.threadpool.IntegerSequenceIterator;
import step.threadpool.ThreadPool;
import step.threadpool.ThreadPool.WorkerController;
import step.threadpool.WorkerItemConsumerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static step.artefacts.handlers.functions.TokenForecastingExecutionPlugin.getTokenForecastingContext;
import static step.artefacts.handlers.functions.TokenForecastingExecutionPlugin.pushNewTokenNumberCalculationContext;

public class ThreadGroupHandler extends ArtefactHandler<ThreadGroup, ReportNode> {

	public void createReportSkeleton_(ReportNode node, ThreadGroup artefact) {
		Integer numberOfThreads = artefact.getUsers().get();

		TokenForecastingContext tokenForecastingContext = getTokenForecastingContext(context);
		pushNewTokenNumberCalculationContext(context, new MultiplyingTokenForecastingContext(tokenForecastingContext, numberOfThreads));

		// The skeleton phase has to be executed within a session to match the behaviour of the execution
		// and properly estimate the required number of tokens
		createReportNodeSkeletonInSession(artefact, node, (sessionArtefact, sessionReportNode) -> {
			SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler(context);
			scheduler.createReportSkeleton_(sessionReportNode, sessionArtefact);
		});
	}

	private void createReportNodeSkeletonInSession(AbstractArtefact artefact, ReportNode node, BiConsumer<AbstractArtefact, ReportNode> consumer, String artefactName, Map<String, Object> newVariables) {
		FunctionGroup functionGroup = createWorkArtefact(FunctionGroup.class, artefact, artefactName, true);
		functionGroup.setConsumer(consumer);
		delegateCreateReportSkeleton(functionGroup, node, newVariables);
	}

	private void createReportNodeSkeletonInSession(AbstractArtefact artefact, ReportNode node, BiConsumer<AbstractArtefact, ReportNode> consumer) {
		createReportNodeSkeletonInSession(artefact, node, consumer, "", new HashMap<>());
	}

	@Override
	public void execute_(final ReportNode node, final ThreadGroup testArtefact) {		
		final int numberOfUsers = testArtefact.getUsers().getOrDefault(Integer.class, 0);
		final int numberOfIterations = testArtefact.getIterations().getOrDefault(Integer.class, 0);
		final int pacing = testArtefact.getPacing().getOrDefault(Integer.class, 0);
		final int rampup = testArtefact.getRampup().getOrDefault(Integer.class, pacing);
		final int pack = testArtefact.getPack().getOrDefault(Integer.class, 1);
		final int maxDuration = testArtefact.getMaxDuration().getOrDefault(Integer.class, 0);
		final int startOffset = testArtefact.getStartOffset().getOrDefault(Integer.class, 0);

		if (numberOfUsers <= 0) {
			throw new RuntimeException("Invalid argument: The number of threads has to be higher than 0.");
		}

		if (maxDuration == 0 && numberOfIterations == 0) {
			throw new RuntimeException(
					"Invalid argument: Either specify the maximum duration or the number of iterations of the thread group.");
		}

		if (pack <= 0) {
			throw new RuntimeException(
					"Invalid argument: The number of packed threads has to be higher than 0.");
		}
		
		// Attach global iteration counter & user counter
		AtomicLong gcounter = new AtomicLong(0);
		AtomicReportNodeStatusComposer reportNodeStatusComposer = new AtomicReportNodeStatusComposer(node);
		
		Iterator<Integer> groupIterator = new IntegerSequenceIterator(1,numberOfUsers,1);
		
		final long groupStartTime = System.currentTimeMillis();
		
		ThreadPool threadPool = context.get(ThreadPool.class);
		threadPool.consumeWork(groupIterator, new WorkerItemConsumerFactory<Integer>() {
			@Override
			public Consumer<Integer> createWorkItemConsumer(WorkerController<Integer> groupController) {
				return groupID -> {
					try {
						final long localStartOffset = startOffset + (long) (1.0 * pack*Math.floor((groupID - 1)/pack) / numberOfUsers * rampup);

						CancellableSleep.sleep(localStartOffset, context::isInterrupted, ThreadGroupHandler.class);

						Thread thread = createWorkArtefact(Thread.class, testArtefact, "Thread "+groupID, true);
						thread.setGcounter(gcounter);
						thread.setGroupController(groupController);
						thread.setGroupId(groupID);
						thread.setGroupStartTime(groupStartTime);
						thread.setNumberOfIterations(numberOfIterations);
						thread.setPacing(pacing);
						thread.setThreadGroup(testArtefact);
						thread.setMaxDuration(maxDuration);
						thread.setBeforeThread(testArtefact.getBeforeThread());
						thread.setAfterThread(testArtefact.getAfterThread());
						HashMap<String, Object> newVariable = new HashMap<>();
						newVariable.put(thread.threadGroup.getUserItem().get(), thread.groupId);
						ReportNode threadReportNode = delegateExecute(thread, node, newVariable);
						reportNodeStatusComposer.addStatusAndRecompose(threadReportNode);
					} catch (Throwable e) {
						failWithException(node, e);
						reportNodeStatusComposer.addStatusAndRecompose(node);
					}
				};
			}
		}, numberOfUsers);

		reportNodeStatusComposer.applyComposedStatusToParentNode(node);
	}

	@Override
	protected List<ResolvedChildren> resolveChildrenArtefactBySource_(ThreadGroup artefactNode, String currentPath) {
		List<ResolvedChildren> results = new ArrayList<>();
		ChildrenBlock beforeThread = artefactNode.getBeforeThread();
		if (beforeThread != null) {
			results.add(new ResolvedChildren(ParentSource.BEFORE_THREAD, beforeThread.getSteps(),currentPath));
		}
		results.add(new ResolvedChildren(ParentSource.MAIN, artefactNode.getChildren(), currentPath));
		ChildrenBlock afterThread = artefactNode.getAfterThread();
		if (afterThread != null) {
			results.add(new ResolvedChildren(ParentSource.AFTER_THREAD, afterThread.getSteps(), currentPath));
		}
		return results;
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, ThreadGroup testArtefact) {
		return new ReportNode();
	}
	
	@Artefact(test = true)
	public static class Thread extends AbstractArtefact {
		
		@JsonIgnore
		int groupId;
		@JsonIgnore
		int numberOfIterations;
		@JsonIgnore
		int pacing;
		@JsonIgnore
		long groupStartTime;
		@JsonIgnore
		ThreadGroup threadGroup;
		@JsonIgnore
		WorkerController<Integer> groupController;
		@JsonIgnore
		AtomicLong gcounter;
		@JsonIgnore
		long maxDuration;
		@JsonIgnore
		ChildrenBlock beforeThread = new ChildrenBlock();
		@JsonIgnore
		ChildrenBlock afterThread = new ChildrenBlock();
		
		public Thread() {
			super();
		}
		
		public int getGroupId() {
			return groupId;
		}

		public void setGroupId(int groupId) {
			this.groupId = groupId;
		}

		public int getNumberOfIterations() {
			return numberOfIterations;
		}

		public void setNumberOfIterations(int numberOfIterations) {
			this.numberOfIterations = numberOfIterations;
		}

		public int getPacing() {
			return pacing;
		}

		public void setPacing(int pacing) {
			this.pacing = pacing;
		}

		public long getGroupStartTime() {
			return groupStartTime;
		}

		public void setGroupStartTime(long groupStartTime) {
			this.groupStartTime = groupStartTime;
		}

		public ThreadGroup getThreadGroup() {
			return threadGroup;
		}

		public void setThreadGroup(ThreadGroup threadGroup) {
			this.threadGroup = threadGroup;
		}

		public WorkerController<Integer> getGroupController() {
			return groupController;
		}

		public void setGroupController(WorkerController<Integer> groupController) {
			this.groupController = groupController;
		}

		public AtomicLong getGcounter() {
			return gcounter;
		}

		public void setGcounter(AtomicLong gcounter) {
			this.gcounter = gcounter;
		}

		public long getMaxDuration() {
			return maxDuration;
		}

		public void setMaxDuration(long maxDuration) {
			this.maxDuration = maxDuration;
		}

		public ChildrenBlock getBeforeThread() {
			return beforeThread;
		}

		public void setBeforeThread(ChildrenBlock beforeThread) {
			this.beforeThread = beforeThread;
		}

		public ChildrenBlock getAfterThread() {
			return afterThread;
		}

		public void setAfterThread(ChildrenBlock afterThread) {
			this.afterThread = afterThread;
		}
	}
	
	public static class ThreadHandler extends AbstractSessionArtefactHandler<Thread, ThreadReportNode> {

		@Override
		protected void createReportSkeleton_(ThreadReportNode parentNode, Thread testArtefact) {
		}

		@Override
		protected void execute_(ThreadReportNode node, Thread thread) {
			int pacing = thread.pacing;
			int numberOfIterations = thread.numberOfIterations;
			long maxDuration = thread.maxDuration;

			ReportNode reportNode = executeInSession(thread, node, (sessionArtefact, sessionReportNode)->{
				AtomicReportNodeStatusComposer sessionReportNodeStatusComposer = new AtomicReportNodeStatusComposer(sessionReportNode);
				try {
					optionalRunChildrenBlock(thread.getBeforeThread(), (before) -> {
						SequentialArtefactScheduler sequentialArtefactScheduler = new SequentialArtefactScheduler(context);
						sequentialArtefactScheduler.execute_(sessionReportNode, before.getSteps(), before.getContinueOnError().get(), ParentSource.BEFORE_THREAD);
						sessionReportNodeStatusComposer.addStatusAndRecompose(sessionReportNode);
					});

					context.getVariablesManager().putVariable(sessionReportNode, TEC_EXECUTION_REPORTNODES_PERSISTBEFORE, false);
					Pacer.scheduleAtConstantPacing(i->{
						ReportNode iterationReportNode = null;
						try {
							long globalCounterValue = thread.gcounter.incrementAndGet();

							Sequence iterationTestCase = createWorkArtefact(Sequence.class, sessionArtefact, "Iteration "+i);

							// Force the persistence of the iteration report node before its execution to have it
							// in the tree view (SED-1002)
							iterationTestCase.addCustomAttribute(ArtefactHandler.FORCE_PERSIST_BEFORE, true);
							for(AbstractArtefact child:sessionArtefact.getChildren()) {
								iterationTestCase.addChild(child);
							}

							HashMap<String, Object> newVariable = new HashMap<>();
							newVariable.put(thread.threadGroup.getLocalItem().get(), i);
							//For Performance reasons, we might want to expose the LongAdder itself rather than calling "intValue()" every time
							newVariable.put(thread.threadGroup.getItem().get(), globalCounterValue);

							iterationReportNode = delegateExecute(iterationTestCase, sessionReportNode, newVariable);
							sessionReportNodeStatusComposer.addStatusAndRecompose(iterationReportNode);
						} catch (Throwable e) {
							if(iterationReportNode!=null) {
								failWithException(iterationReportNode, e);
								sessionReportNodeStatusComposer.addStatusAndRecompose(iterationReportNode);
							}
						}

					}, pacing,
							c -> !context.isInterrupted()
									&& (maxDuration == 0 || c.getDuration() < maxDuration)
									&& (numberOfIterations == 0 || c.getIterations() < numberOfIterations), context);
				} catch (InterruptedException e) {
					failWithException(sessionReportNode, e);
					sessionReportNodeStatusComposer.addStatusAndRecompose(sessionReportNode);
				} finally {
					// Execute the After Thread steps artefacts even when aborting
					boolean byPassInterrupt = context.isInterrupted();
					if (byPassInterrupt) {
						context.byPassInterruptInCurrentThread(true);
					}
					try{
						optionalRunChildrenBlock(thread.getAfterThread(), (after) -> {
							SequentialArtefactScheduler sequentialArtefactScheduler = new SequentialArtefactScheduler(context);
							sequentialArtefactScheduler.execute_(sessionReportNode, after.getSteps(), after.getContinueOnError().get(), ParentSource.AFTER_THREAD);
							sessionReportNodeStatusComposer.addStatusAndRecompose(sessionReportNode);
						});
					} finally {
						//resume aborting if required
						if (byPassInterrupt) {
							context.byPassInterruptInCurrentThread(false);
						}
					}
				}
				sessionReportNodeStatusComposer.applyComposedStatusToParentNode(sessionReportNode);
			});
			
			node.setStatus(reportNode.getStatus());
		}

		@Override
		public ThreadReportNode createReportNode_(ReportNode parentNode, Thread testArtefact) {
			ThreadReportNode threadReportNode = new ThreadReportNode();
			threadReportNode.setThreadGroupName((parentNode != null) ? parentNode.getName() : "Unnamed");
			return threadReportNode;
		}
	}
}
