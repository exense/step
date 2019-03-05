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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import step.artefacts.Sequence;
import step.artefacts.ThreadGroup;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactAccessor;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicValue;

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
		// Using bindings instead
		//context.getVariablesManager().putVariable(node, testArtefact.getItem().get(), gcounter);
		
		AtomicReportNodeStatusComposer reportNodeStatusComposer = new AtomicReportNodeStatusComposer(node.getStatus());
		
		ExecutorService executor = Executors.newFixedThreadPool(numberOfUsers);
		try {
			final long groupStartTime = System.currentTimeMillis();
			
			// -- Paralellize --
			for(int j=0;j<numberOfUsers;j++) {
				final int groupID = j;				
				final long localStartOffset = testArtefact.getStartOffset().get()+(long)((1.0*groupID)/numberOfUsers*rampup);
				executor.submit(new Runnable() {
					public void run() {
						context.associateThread();
						
						ReportNode iterationReportNode = null;
						try {
							Thread.sleep(localStartOffset);
							
							// -- Iterate --
							for(int i=0;i<numberOfIterations;i++) {
								long startTime = System.currentTimeMillis();
								
								gcounter.increment();
								
								if(context.isInterrupted()) {
									break;
								}
								
								ArtefactAccessor artefactAccessor = context.getArtefactAccessor();
								Sequence iterationTestCase = artefactAccessor.createWorkArtefact(Sequence.class, testArtefact, "Group_"+groupID+"_Iteration_"+i);
								
								for(AbstractArtefact child:getChildren(testArtefact)) {
									iterationTestCase.addChild(child.getId());
								}
								
								HashMap<String, Object> newVariable = new HashMap<>();
								newVariable.put(testArtefact.getLocalItem().get(), i);
								newVariable.put(testArtefact.getUserItem().get(), groupID);
								//For Performance reasons, we might want to expose the LongAdder itself rather than calling "intValue()" every time
								newVariable.put(testArtefact.getItem().get(), gcounter.intValue());

								iterationReportNode = delegateExecute(context, iterationTestCase, node, newVariable);
								reportNodeStatusComposer.addStatusAndRecompose(iterationReportNode.getStatus());
								
								long endTime = System.currentTimeMillis();
								if(pacing!=0) {
									long duration = endTime-startTime;
									long pacingWait = pacing-duration;
									if(pacingWait>0) {
										Thread.sleep(pacingWait);
									} else {
										// TODO: this is an application warning. Instead of being logged it should be shown to the end-user in a warning console
										logger.debug("Pacing of TestGroup " + testArtefact.getId() + " in test " + context.getExecutionId() + " exceeded. " +
												"The iteration lasted " + duration + "ms. The defined pacing was: " + testArtefact.getPacing() + "ms.");
									}
								}
								
								DynamicValue<Integer> maxDurationProp = testArtefact.getMaxDuration();
								if(maxDurationProp != null) {
									Integer maxDuration = maxDurationProp.get();
									if(maxDuration > 0 && endTime>groupStartTime+maxDuration) {
										break;
									}
								}
							}
						} catch (Exception e) {
							if(iterationReportNode!=null) {
								failWithException(iterationReportNode, e);
								reportNodeStatusComposer.addStatusAndRecompose(iterationReportNode.getStatus());
							}
						}
					}
				});
			}
			
			executor.shutdown();
			executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
			node.setStatus(reportNodeStatusComposer.getParentStatus());
		} catch (InterruptedException e) {
			failWithException(node, e);
		} finally {
			executor.shutdownNow();
		}
		
		
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, ThreadGroup testArtefact) {
		return new ReportNode();
	}
	
	
	

}
