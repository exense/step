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

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonIgnore;

import step.artefacts.AfterThread;
import step.artefacts.BeforeThread;
import step.artefacts.Sequence;
import step.artefacts.ThreadGroup;
import step.artefacts.handlers.loadtesting.Pacer;
import step.artefacts.reports.ThreadReportNode;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.handlers.AtomicReportNodeStatusComposer;
import step.core.artefacts.reports.ReportNode;
import step.threadpool.IntegerSequenceIterator;
import step.threadpool.ThreadPool;
import step.threadpool.ThreadPool.WorkerController;
import step.threadpool.WorkerItemConsumerFactory;

public class ThreadGroupHandler extends ArtefactHandler<ThreadGroup, ReportNode> {
	
	public void createReportSkeleton_(ReportNode node, ThreadGroup testArtefact) {		
	}

	@Override
	public void execute_(final ReportNode node, final ThreadGroup testArtefact) {		
		final int numberOfUsers = testArtefact.getUsers().getOrDefault(0);
		final int numberOfIterations = testArtefact.getIterations().getOrDefault(0);
		final int pacing = testArtefact.getPacing().getOrDefault(0);
		final long rampup = testArtefact.getRampup().getOrDefault(pacing);
		final int maxDuration = testArtefact.getMaxDuration().getOrDefault(0);
		final int startOffset = testArtefact.getStartOffset().getOrDefault(0);

		if (numberOfUsers <= 0) {
			throw new RuntimeException("Invalid argument: The number of threads has to be higher than 0.");
		}
		if (maxDuration == 0 && numberOfIterations == 0) {
			throw new RuntimeException(
					"Invalid argument: Either specify the maximum duration or the number of iterations of the thread group.");
		}
		
		// Attach global iteration counter & user counter
		LongAdder gcounter = new LongAdder();
		AtomicReportNodeStatusComposer reportNodeStatusComposer = new AtomicReportNodeStatusComposer(node.getStatus());
		
		Iterator<Integer> groupIterator = new IntegerSequenceIterator(1,numberOfUsers,1);
		
		final long groupStartTime = System.currentTimeMillis();
		
		ThreadPool threadPool = context.get(ThreadPool.class);
		threadPool.consumeWork(groupIterator, new WorkerItemConsumerFactory<Integer>() {
			@Override
			public Consumer<Integer> createWorkItemConsumer(WorkerController<Integer> groupController) {
				return groupID -> {
					try {
						final long localStartOffset = startOffset + (long) ((1.0 * (groupID - 1)) / numberOfUsers * rampup);

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
						
						HashMap<String, Object> newVariable = new HashMap<>();
						newVariable.put(thread.threadGroup.getUserItem().get(), thread.groupId);
						ReportNode threadReportNode = delegateExecute(thread, node, newVariable);
						reportNodeStatusComposer.addStatusAndRecompose(threadReportNode.getStatus());
					} catch (Exception e) {
						failWithException(node, e);
						reportNodeStatusComposer.addStatusAndRecompose(node.getStatus());
					}
				};
			}
		}, numberOfUsers);
		
		node.setStatus(reportNodeStatusComposer.getParentStatus());
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, ThreadGroup testArtefact) {
		return new ReportNode();
	}
	
	@Artefact()
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
		LongAdder gcounter;
		@JsonIgnore
		long maxDuration;
		
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

		public LongAdder getGcounter() {
			return gcounter;
		}

		public void setGcounter(LongAdder gcounter) {
			this.gcounter = gcounter;
		}

		public long getMaxDuration() {
			return maxDuration;
		}

		public void setMaxDuration(long maxDuration) {
			this.maxDuration = maxDuration;
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
				SequentialArtefactScheduler sequentialArtefactScheduler = new SequentialArtefactScheduler(context);
				sequentialArtefactScheduler.executeWithinBeforeAndAfter(sessionArtefact, sessionReportNode, newChildren->{
					AtomicReportNodeStatusComposer sessionReportNodeStatusComposer = new AtomicReportNodeStatusComposer(sessionReportNode.getStatus());
					try {
						Pacer.scheduleAtConstantPacing(i->{
							ReportNode iterationReportNode = null;
							try {
								thread.gcounter.increment();
								
								Sequence iterationTestCase = createWorkArtefact(Sequence.class, sessionArtefact, "Iteration "+i);
								
								for(AbstractArtefact child:newChildren) {
									iterationTestCase.addChild(child);
								}
								
								HashMap<String, Object> newVariable = new HashMap<>();
								newVariable.put(thread.threadGroup.getLocalItem().get(), i);
								//For Performance reasons, we might want to expose the LongAdder itself rather than calling "intValue()" every time
								newVariable.put(thread.threadGroup.getItem().get(), thread.gcounter.intValue());
								
								iterationReportNode = delegateExecute(iterationTestCase, sessionReportNode, newVariable);
								sessionReportNodeStatusComposer.addStatusAndRecompose(iterationReportNode.getStatus());
							} catch (Exception e) {
								if(iterationReportNode!=null) {
									failWithException(iterationReportNode, e);
									sessionReportNodeStatusComposer.addStatusAndRecompose(iterationReportNode.getStatus());
								}
							}
							
						}, pacing,
								c -> !context.isInterrupted()
										&& (maxDuration == 0 || c.getDuration() < maxDuration)
										&& (numberOfIterations == 0 || c.getIterations() < numberOfIterations));
					} catch (InterruptedException e) {
						failWithException(sessionReportNode, e);
						sessionReportNodeStatusComposer.addStatusAndRecompose(sessionReportNode.getStatus());
					}
					sessionReportNode.setStatus(sessionReportNodeStatusComposer.getParentStatus());
					return sessionReportNode;
				}, BeforeThread.class, AfterThread.class);
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
