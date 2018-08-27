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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import step.artefacts.AbstractForBlock;
import step.artefacts.Sequence;
import step.artefacts.reports.ForBlockReportNode;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactAccessor;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.datapool.DataPoolFactory;
import step.datapool.DataPoolRow;
import step.datapool.DataSet;

public class ForBlockHandler extends ArtefactHandler<AbstractForBlock, ForBlockReportNode> {
	
	private static final String BREAK_VARIABLE = "break";
	
	@Override
	public void createReportSkeleton_(ForBlockReportNode node, AbstractForBlock testArtefact) {		
		List<AbstractArtefact> selectedChildren = getChildren(testArtefact);
		DataSet<?> dataSet = null;
		try {
			dataSet = DataPoolFactory.getDataPool(testArtefact.getDataSourceType(), testArtefact.getDataSource(), context);
			dataSet.init();
			DataPoolRow nextValue = null;
			int rowCount = 0;
			while((nextValue=dataSet.next())!=null) {				
				if(context.isInterrupted()) {
					break;
				}
	
				rowCount++;
				
				HashMap<String, Object> newVariable = new HashMap<>();
				newVariable.put(testArtefact.getItem().get(), nextValue.getValue());
				
				ArtefactAccessor artefactAccessor = context.getArtefactAccessor();
				Sequence iterationTestCase = artefactAccessor.createWorkArtefact(Sequence.class, testArtefact, "Iteration_"+rowCount);
				for(AbstractArtefact child:selectedChildren)
					iterationTestCase.addChild(child.getId());
				
				delegateCreateReportSkeleton(context, iterationTestCase, node, newVariable);
			}
		} catch(Exception e) {
			failWithException(node, e);
		} finally {
			if(dataSet!=null) {
				dataSet.close();
			}
		}
	}

	@Override
	public void execute_(ForBlockReportNode node, AbstractForBlock testArtefact) {
				
		DataSet<?> dataSet = null;
		try {
			List<AbstractArtefact> selectedChildren = getChildren(testArtefact);
			
			dataSet = DataPoolFactory.getDataPool(testArtefact.getDataSourceType(), testArtefact.getDataSource(), context);
		
			dataSet.init();
			
			context.getVariablesManager().putVariable(node, BREAK_VARIABLE, "false");
			
			AtomicInteger failedLoopsCounter = new AtomicInteger();
			AtomicInteger loopsCounter = new AtomicInteger();
			IterationRunnable iterationRunnable = new IterationRunnable(testArtefact, selectedChildren, node, dataSet, failedLoopsCounter, loopsCounter);
			
			Integer numberOfThreads = testArtefact.getThreads().get();
			if(numberOfThreads>1) {
				ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
				for(int i=0;i<numberOfThreads;i++) {
					executor.submit(iterationRunnable);
				}
				
				executor.shutdown();
				executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
			} else {
				iterationRunnable.run();
			}
			
			node.setErrorCount(failedLoopsCounter.get());
			node.setCount(loopsCounter.get());
			if(failedLoopsCounter.get()>0) {
				node.setStatus(ReportNodeStatus.FAILED);
			} else {
				node.setStatus(ReportNodeStatus.PASSED);
			}
		} catch(Exception e) {
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
	
	private class IterationRunnable implements Runnable {
		
		private final DataSet<?> dataSet;
		private final AtomicInteger failedLoops;
		private final ReportNode node;
		private final AbstractForBlock testArtefact;
		private final AtomicInteger loopsCounter;
		private final List<AbstractArtefact> selectedChildren;

		public IterationRunnable(AbstractForBlock testArtefact, List<AbstractArtefact> selectedChildren, ReportNode node, DataSet<?> dataSet, AtomicInteger failedLoops, AtomicInteger loopsCounter) {
			super();
			this.testArtefact = testArtefact;
			this.node = node;
			this.dataSet = dataSet;
			this.failedLoops = failedLoops;
			this.loopsCounter = loopsCounter;
			this.selectedChildren = selectedChildren;
		}

		@Override
		public void run() {
			context.associateThread();
			
			try {
				DataPoolRow nextValue = null;
				while((nextValue=dataSet.next())!=null) {	
					boolean forInterrupted = Boolean.parseBoolean((String)context.getVariablesManager().getVariable(node, BREAK_VARIABLE, false));
					Integer maxFailedLoops = testArtefact.getMaxFailedLoops().get();
					if(forInterrupted || context.isInterrupted() || (maxFailedLoops!=null&&failedLoops.get()>=maxFailedLoops)) {
						break;
					}
					int i = loopsCounter.incrementAndGet();
	
					HashMap<String, Object> newVariable = new HashMap<>();
					newVariable.put(testArtefact.getItem().get(), nextValue.getValue());
					
					ArtefactAccessor artefactAccessor = context.getArtefactAccessor();
					Sequence iterationTestCase = artefactAccessor.createWorkArtefact(Sequence.class, testArtefact, "Iteration"+i);
					for(AbstractArtefact child:selectedChildren) {
						iterationTestCase.addChild(child.getId());
					}
					
					ReportNode iterationReportNode = delegateExecute(context, iterationTestCase, node, newVariable);
					
					if(iterationReportNode.getStatus()==ReportNodeStatus.TECHNICAL_ERROR || iterationReportNode.getStatus()==ReportNodeStatus.FAILED) {
						failedLoops.incrementAndGet();
					}
				}
			} catch(Exception e) {
				failWithException(node, e);
			}
		}
	}
	
	@Override
	public ForBlockReportNode createReportNode_(ReportNode parentNode, AbstractForBlock testArtefact) {
		return new ForBlockReportNode();
	}
}
