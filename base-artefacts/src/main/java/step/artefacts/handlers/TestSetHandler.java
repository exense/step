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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import step.artefacts.TestSet;
import step.artefacts.handlers.scheduler.DefaultTestSetScheduler;
import step.artefacts.handlers.scheduler.TestCaseBundle;
import step.artefacts.handlers.scheduler.TestSetScheduler;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionManager;

public class TestSetHandler extends ArtefactHandler<TestSet, ReportNode> {
	
	@Override
	public void createReportSkeleton_(ReportNode node, TestSet testSet) {	
		ExecutionManager executionManager = new ExecutionManager(context.getExecutionAccessor());
		executionManager.updateExecutionType(context, "TestSet");
		runParallel(node, testSet, false);
	}

	@Override
	public void execute_(ReportNode node, TestSet testSet) {
		runParallel(node, testSet, true);
	}

	private void runParallel(ReportNode node, TestSet testSet, boolean execution) {
		int numberOfThreads = context.getVariablesManager().getVariableAsInteger("tec.execution.threads",1);

		AtomicReportNodeStatusComposer reportNodeStatusComposer = new AtomicReportNodeStatusComposer(ReportNodeStatus.PASSED);
		
		TestSetScheduler scheduler = new DefaultTestSetScheduler();
		List<TestCaseBundle> bundles = scheduler.bundleTestCases(getChildren(testSet), numberOfThreads);
		ExecutorService executor = Executors.newFixedThreadPool(bundles.size());
		try {
			for(TestCaseBundle bundle:bundles) {
				BundleProcessor bundleProcessor = new BundleProcessor(testSet, bundle, node, execution, reportNodeStatusComposer);
				executor.submit(bundleProcessor);
			}
			
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
			
			// throws the first exception found
			for(TestCaseBundle bundle:bundles) {
				if(bundle.getThrowable()!=null) {
					throw bundle.getThrowable();
				}
			}
			
			ReportNodeStatus status = reportNodeStatusComposer.getParentStatus();
			
			node.setStatus(status);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			executor.shutdownNow();
		}
	}
	
	private class BundleProcessor implements Runnable {
		
		private boolean execution;
		
		private TestCaseBundle bundle;
		
		private ReportNode node;
		
		private AtomicReportNodeStatusComposer reportNodeStatusComposer;

		public BundleProcessor(TestSet testSet, TestCaseBundle bundle, ReportNode node, boolean execution, AtomicReportNodeStatusComposer reportNodeStatusComposer) {
			super();
			this.bundle = bundle;
			this.node = node;
			this.execution = execution;
			this.reportNodeStatusComposer = reportNodeStatusComposer;
		}

		@Override
		public void run() {
			context.associateThread();
			
			try {
				for(AbstractArtefact testArtefact:bundle.getTestcases()) {
					if(context.isInterrupted()) {
						break;
					}				
					if(execution) {
						ReportNode resultNode = delegateExecute(testArtefact, node);
						reportNodeStatusComposer.addStatusAndRecompose(resultNode.getStatus());
					} else {
						delegateCreateReportSkeleton(testArtefact, node);
					}
				}
			} catch (Throwable e) {
				bundle.setThrowable(e);
			}
		}

	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, TestSet testArtefact) {
		return new ReportNode();
	}
}
