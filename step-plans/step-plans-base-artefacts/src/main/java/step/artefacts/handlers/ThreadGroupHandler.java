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

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonIgnore;

import step.artefacts.Sequence;
import step.artefacts.ThreadGroup;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicValue;
import step.threadpool.IntegerSequenceIterator;
import step.threadpool.ThreadPool;
import step.threadpool.ThreadPool.WorkerController;
import step.threadpool.WorkerItemConsumerFactory;

public class ThreadGroupHandler extends ArtefactHandler<ThreadGroup, ReportNode> {
	
	public void createReportSkeleton_(ReportNode node, ThreadGroup testArtefact) {		
	}

	@Override
	public void execute_(final ReportNode node, final ThreadGroup testArtefact) {		
		final Integer numberOfUsers = testArtefact.getUsers().get();
		if(numberOfUsers==null||numberOfUsers<=0) {
			throw new RuntimeException("Invalid argument: 'users' has to be higher than 0.");
		}
		
		final int numberOfIterations = testArtefact.getIterations().get();
		final int pacing;
		if(testArtefact.getPacing().get()!=null) {
			pacing = testArtefact.getPacing().get();
		} else {
			pacing = 0;
		}
		final long rampup;
		if(testArtefact.getRampup().get()!=null) {
			rampup = testArtefact.getRampup().get();
		} else {
			rampup = pacing;
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
						final long localStartOffset = testArtefact.getStartOffset().get()+(long)((1.0*groupID)/numberOfUsers*rampup);

						try {
							java.lang.Thread.sleep(localStartOffset);
						} catch (InterruptedException e1) {
							throw new RuntimeException(e1);
						}
						
						Thread thread = createWorkArtefact(Thread.class, testArtefact, "Thread "+groupID, true);
						thread.setGcounter(gcounter);
						thread.setGroupController(groupController);
						thread.setGroupId(groupID);
						thread.setGroupStartTime(groupStartTime);
						thread.setNumberOfIterations(numberOfIterations);
						thread.setPacing(pacing);
						thread.setThreadGroup(testArtefact);
						
						ReportNode threadReportNode = delegateExecute(context, thread, node, new HashMap<>());
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
	
	@Artefact(handler = ThreadHandler.class)
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
	}
	
	public static class ThreadHandler extends AbstractSessionArtefactHandler<Thread, ReportNode> {

		@Override
		protected void createReportSkeleton_(ReportNode parentNode, Thread testArtefact) {
			
		}

		@Override
		protected void execute_(ReportNode node, Thread thread) throws Exception {
			ThreadPool threadPool = context.get(ThreadPool.class);
			
			ReportNode reportNode = executeInSession(thread, node, (sessionArtefact, sessionReportNode)->{
				AtomicReportNodeStatusComposer sessionReportNodeStatusComposer = new AtomicReportNodeStatusComposer(sessionReportNode.getStatus());
				IntegerSequenceIterator iterationIterator = new IntegerSequenceIterator(1, thread.numberOfIterations, 1);
				threadPool.consumeWork(iterationIterator, new WorkerItemConsumerFactory<Integer>() {
					@Override
					public Consumer<Integer> createWorkItemConsumer(WorkerController<Integer> iterationController) {
						return i -> {
							ReportNode iterationReportNode = null;
							try {
								thread.gcounter.increment();
								
								Sequence iterationTestCase = createWorkArtefact(Sequence.class, sessionArtefact, "Iteration "+i);
								
								if(thread.pacing!=0) {
									iterationTestCase.setPacing(new DynamicValue<Long>((long)thread.pacing));
								}
								
								for(AbstractArtefact child:getChildren(thread)) {
									iterationTestCase.addChild(child);
								}
								
								HashMap<String, Object> newVariable = new HashMap<>();
								newVariable.put(thread.threadGroup.getLocalItem().get(), i);
								newVariable.put(thread.threadGroup.getUserItem().get(), thread.groupId);
								//For Performance reasons, we might want to expose the LongAdder itself rather than calling "intValue()" every time
								newVariable.put(thread.threadGroup.getItem().get(), thread.gcounter.intValue());

								iterationReportNode = delegateExecute(context, iterationTestCase, sessionReportNode, newVariable);
								sessionReportNodeStatusComposer.addStatusAndRecompose(iterationReportNode.getStatus());
																
								DynamicValue<Integer> maxDurationProp = thread.threadGroup.getMaxDuration();
								if(maxDurationProp != null) {
									Integer maxDuration = maxDurationProp.get();
									if(maxDuration > 0 && System.currentTimeMillis()>thread.groupStartTime+maxDuration) {
										iterationController.interrupt();
										thread.groupController.interrupt();
									}
								}
							} catch (Exception e) {
								if(iterationReportNode!=null) {
									failWithException(iterationReportNode, e);
									sessionReportNodeStatusComposer.addStatusAndRecompose(iterationReportNode.getStatus());
								}
							}
						};
					}
				}, 1);
				sessionReportNode.setStatus(sessionReportNodeStatusComposer.getParentStatus());
			});
			
			node.setStatus(reportNode.getStatus());
		}

		@Override
		public ReportNode createReportNode_(ReportNode parentNode, Thread testArtefact) {
			return new ReportNode();
		}
	}
}
