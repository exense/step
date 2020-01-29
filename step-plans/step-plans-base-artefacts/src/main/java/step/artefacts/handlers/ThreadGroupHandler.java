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

import step.artefacts.Sequence;
import step.artefacts.ThreadGroup;
import step.core.artefacts.AbstractArtefact;
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
							Thread.sleep(localStartOffset);
						} catch (InterruptedException e1) {
							throw new RuntimeException(e1);
						}
						
						IntegerSequenceIterator iterationIterator = new IntegerSequenceIterator(1, numberOfIterations, 1);
						threadPool.consumeWork(iterationIterator, new WorkerItemConsumerFactory<Integer>() {
							@Override
							public Consumer<Integer> createWorkItemConsumer(WorkerController<Integer> iterationController) {
								return i -> {
									ReportNode iterationReportNode = null;
									try {
										gcounter.increment();
										
										Sequence iterationTestCase = createWorkArtefact(Sequence.class, testArtefact, "Group_"+groupID+"_Iteration_"+i);
										
										if(pacing!=0) {
											iterationTestCase.setPacing(new DynamicValue<Long>((long)pacing));
										}
										
										for(AbstractArtefact child:getChildren(testArtefact)) {
											iterationTestCase.addChild(child);
										}
										
										HashMap<String, Object> newVariable = new HashMap<>();
										newVariable.put(testArtefact.getLocalItem().get(), i);
										newVariable.put(testArtefact.getUserItem().get(), groupID);
										//For Performance reasons, we might want to expose the LongAdder itself rather than calling "intValue()" every time
										newVariable.put(testArtefact.getItem().get(), gcounter.intValue());
		
										iterationReportNode = delegateExecute(context, iterationTestCase, node, newVariable);
										reportNodeStatusComposer.addStatusAndRecompose(iterationReportNode.getStatus());
																		
										DynamicValue<Integer> maxDurationProp = testArtefact.getMaxDuration();
										if(maxDurationProp != null) {
											Integer maxDuration = maxDurationProp.get();
											if(maxDuration > 0 && System.currentTimeMillis()>groupStartTime+maxDuration) {
												iterationController.interrupt();
												groupController.interrupt();
											}
										}
									} catch (Exception e) {
										if(iterationReportNode!=null) {
											failWithException(iterationReportNode, e);
											reportNodeStatusComposer.addStatusAndRecompose(iterationReportNode.getStatus());
										}
									}
								};
							}
						}, 1);
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
}
