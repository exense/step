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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import step.artefacts.AbstractForBlock;
import step.artefacts.Sequence;
import step.artefacts.reports.ForBlockReportNode;
import step.core.artefacts.handlers.AtomicReportNodeStatusComposer;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.datapool.DataPoolFactory;
import step.datapool.DataPoolRow;
import step.datapool.DataSet;
import step.threadpool.ThreadPool;
import step.threadpool.ThreadPool.WorkerController;
import step.threadpool.WorkerItemConsumerFactory;

public class ForBlockHandler extends AbstractSessionArtefactHandler<AbstractForBlock, ForBlockReportNode> {
	
	private static final String BREAK_VARIABLE = "break";
	
	@Override
	public void createReportSkeleton_(ForBlockReportNode node, AbstractForBlock testArtefact) {		
		DataSet<?> dataSet = null;
		try {
			dataSet = getDataPool(testArtefact);
			DataPoolRow nextValue = null;
			int rowCount = 0;
			while((nextValue=dataSet.next())!=null) {				
				try {
					if(context.isInterrupted()) {
						break;
					}
					
					rowCount++;
					
					HashMap<String, Object> newVariable = new HashMap<>();
					newVariable.put(testArtefact.getItem().get(), nextValue.getValue());
					newVariable.put(testArtefact.getGlobalCounter().get(), rowCount);
					newVariable.put(testArtefact.getUserItem().get(), 1);
					
					createReportNodeSkeletonInSession(testArtefact, node, (sessionArtefact, sessionReportNode)->{
						SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler(context);
						scheduler.execute_(sessionReportNode, sessionArtefact);
					}, "Iteration "+rowCount, newVariable);
				} finally {
					nextValue.commit();
				}
			}
		} catch(Throwable e) {
			failWithException(node, e);
		} finally {
			if(dataSet!=null) {
				dataSet.close();
			}
		}
	}

	public DataSet<?> getDataPool(AbstractForBlock testArtefact) {
		DataSet<?> dataSet;
		dataSet = DataPoolFactory.getDataPool(testArtefact.getDataSourceType(), testArtefact.getDataSource(), context);
		dataSet.enableRowCommit(true);
		dataSet.init();
		return dataSet;
	}

	@Override
	public void execute_(ForBlockReportNode node, AbstractForBlock testArtefact) {
		final DataSet<?> dataSet = getDataPool(testArtefact);
		try {
			Iterator<DataPoolRow> workItemIterator = new Iterator<DataPoolRow>() {

				@Override
				public boolean hasNext() {
					return true;
				}

				@Override
				public DataPoolRow next() {
					return dataSet.next();
				}
			};
			
			context.getVariablesManager().putVariable(node, BREAK_VARIABLE, "false");
			
			AtomicInteger failedLoopsCounter = new AtomicInteger();
			AtomicInteger loopsCounter = new AtomicInteger();
			AtomicReportNodeStatusComposer reportNodeStatusComposer = new AtomicReportNodeStatusComposer(ReportNodeStatus.NORUN);
			
			Integer numberOfThreads = testArtefact.getThreads().get();
			
			ThreadPool threadPool = context.get(ThreadPool.class);
			threadPool.consumeWork(workItemIterator, new WorkerItemConsumerFactory<DataPoolRow>() {
				@Override
				public Consumer<DataPoolRow> createWorkItemConsumer(WorkerController<DataPoolRow> control) {
					return workItem -> {
						try {
							int i = loopsCounter.incrementAndGet();

							HashMap<String, Object> newVariable = new HashMap<>();
							newVariable.put(testArtefact.getItem().get(), workItem.getValue());
							newVariable.put(testArtefact.getGlobalCounter().get(), i);
							newVariable.put(testArtefact.getUserItem().get(), control.getWorkerId());
							
							ReportNode iterationReportNode;
							if(control.isParallel()) {
								iterationReportNode = executeInSession(testArtefact, node, (sessionArtefact, sessionReportNode)->{
									SequentialArtefactScheduler scheduler = new SequentialArtefactScheduler(context);
									scheduler.execute_(sessionReportNode, sessionArtefact);
								}, "Iteration "+i, newVariable);
							} else {
								Sequence iterationTestCase = createWorkArtefact(Sequence.class, testArtefact, "Iteration "+i, true);
								iterationReportNode = delegateExecute(iterationTestCase, node, newVariable);
							}
							
							reportNodeStatusComposer.addStatusAndRecompose(iterationReportNode.getStatus());
							
							if(iterationReportNode.getStatus()==ReportNodeStatus.TECHNICAL_ERROR || iterationReportNode.getStatus()==ReportNodeStatus.FAILED) {
								failedLoopsCounter.incrementAndGet();
							}

							boolean forInterrupted = Boolean.parseBoolean((String)context.getVariablesManager().getVariable(node, BREAK_VARIABLE, false));
							Integer maxFailedLoops = testArtefact.getMaxFailedLoops().get();
							if(forInterrupted || (maxFailedLoops!=null&&failedLoopsCounter.get()>=maxFailedLoops)) {
								control.interrupt();
							}
						} catch(Throwable e) {
							failWithException(node, e);
						} finally {
							workItem.commit();
						}
					};
				}
			}, numberOfThreads);
			
			node.setErrorCount(failedLoopsCounter.get());
			node.setCount(loopsCounter.get());
			node.setStatus(reportNodeStatusComposer.getParentStatus());
		} catch(Throwable e) {
			failWithException(node, e);
		} finally {
			if(dataSet!=null) {
				try {
					dataSet.save();
				} finally {
					dataSet.close();					
				}
			}
		}
	}
	
	@Override
	public ForBlockReportNode createReportNode_(ReportNode parentNode, AbstractForBlock testArtefact) {
		return new ForBlockReportNode();
	}
}
