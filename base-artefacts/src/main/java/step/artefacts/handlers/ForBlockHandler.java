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
import step.core.miscellaneous.TestArtefactResultHandler;
import step.datapool.DataPoolFactory;
import step.datapool.DataPoolRow;
import step.datapool.DataSet;

public class ForBlockHandler extends ArtefactHandler<AbstractForBlock, ForBlockReportNode> {
	
	private static final String BREAK_VARIABLE = "break";
	
	@Override
	public void createReportSkeleton_(ForBlockReportNode node, AbstractForBlock testArtefact) {		
		List<AbstractArtefact> selectedChildren = getChildren(testArtefact);
		DataSet dataSet = null;
		try {
			dataSet = DataPoolFactory.getDataPool(testArtefact.getDataSourceType(), testArtefact.getDataSource());
			dataSet.reset();
			DataPoolRow nextValue = null;
			int count = 0;
			while((nextValue=dataSet.next())!=null && count<asInteger(testArtefact.getMaxLoops(),Integer.MAX_VALUE)) {				
				if(context.isInterrupted()) {
					break;
				}
				
				count++;
	
				HashMap<String, Object> newVariable = new HashMap<>();
				newVariable.put(testArtefact.getItem(), nextValue.getValue());
				
				ArtefactAccessor artefactAccessor = context.getGlobalContext().getArtefactAccessor();
				Sequence iterationTestCase = artefactAccessor.createWorkArtefact(Sequence.class, testArtefact, "Iteration"+nextValue.getRowNum());
				for(AbstractArtefact child:selectedChildren) {
					iterationTestCase.addChild(child.getId());
				}
				
				delegateCreateReportSkeleton(iterationTestCase, node, newVariable);
			}
		} catch(Exception e) {
			TestArtefactResultHandler.failWithException(node, e);
		} finally {
			if(dataSet!=null) {
				dataSet.close();
			}
		}
	}

	@Override
	public void execute_(ForBlockReportNode node, AbstractForBlock testArtefact) {
				
		DataSet dataSet = null;
		try {
			List<AbstractArtefact> selectedChildren = getChildren(testArtefact);
			
			dataSet = DataPoolFactory.getDataPool(testArtefact.getDataSourceType(), testArtefact.getDataSource());
		
			dataSet.reset();
			
			context.getVariablesManager().putVariable(node, BREAK_VARIABLE, "false");
			
			AtomicInteger failedLoopsCounter = new AtomicInteger();
			AtomicInteger loopsCounter = new AtomicInteger();
			IterationRunnable iterationRunnable = new IterationRunnable(testArtefact, selectedChildren, node, dataSet, failedLoopsCounter, loopsCounter);
			if(testArtefact.getParallel()!=null && asBoolean(testArtefact.getParallel(),null)) {
				int threads;
				if(testArtefact.getThreads()!=null) {
					threads = asInteger(testArtefact.getThreads(),null);
				} else {
					threads = ExecutionContext.getCurrentContext().getVariablesManager().getVariableAsInteger("tec.handlers.for.threads.default");
				}
				
				ExecutorService executor = Executors.newFixedThreadPool(threads);
				for(int i=0;i<threads;i++) {
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
			TestArtefactResultHandler.failWithException(node, e);
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
		
		private final DataSet dataSet;
		private final AtomicInteger failedLoops;
		private final AtomicInteger loopsCounter;
		private final ReportNode node;
		private final AbstractForBlock testArtefact;
		private final List<AbstractArtefact> selectedChildren;

		public IterationRunnable(AbstractForBlock testArtefact, List<AbstractArtefact> selectedChildren, ReportNode node, DataSet dataSet, AtomicInteger failedLoops, AtomicInteger loopsCounter) {
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
			ExecutionContext.setCurrentContext(context);
			
			try {
				DataPoolRow nextValue = null;
				while((nextValue=dataSet.next())!=null) {			
					boolean forInterrupted = Boolean.parseBoolean((String)context.getVariablesManager().getVariable(node, BREAK_VARIABLE, false));
					if(forInterrupted || context.isInterrupted() || failedLoops.get()>=asInteger(testArtefact.getMaxFailedLoops(), Integer.MAX_VALUE)|| loopsCounter.incrementAndGet()>=asInteger(testArtefact.getMaxLoops(),Integer.MAX_VALUE)) {
						break;
					}
	
					HashMap<String, Object> newVariable = new HashMap<>();
					newVariable.put(testArtefact.getItem(), nextValue.getValue());
					
					ArtefactAccessor artefactAccessor = context.getGlobalContext().getArtefactAccessor();
					Sequence iterationTestCase = artefactAccessor.createWorkArtefact(Sequence.class, testArtefact, "Iteration"+nextValue.getRowNum());
					for(AbstractArtefact child:selectedChildren) {
						iterationTestCase.addChild(child.getId());
					}
					
					ReportNode iterationReportNode = delegateExecute(iterationTestCase, node, newVariable);
					
					if(iterationReportNode.getStatus()==ReportNodeStatus.TECHNICAL_ERROR || iterationReportNode.getStatus()==ReportNodeStatus.FAILED) {
						failedLoops.incrementAndGet();
					}
				}
			} catch(Exception e) {
				TestArtefactResultHandler.failWithException(node, e);
			}
		}
	}
	
	@Override
	public ForBlockReportNode createReportNode_(ReportNode parentNode, AbstractForBlock testArtefact) {
		return new ForBlockReportNode();
	}
}
