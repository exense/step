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
import step.core.execution.ExecutionContext;

public class TestSetHandler extends ArtefactHandler<TestSet, ReportNode> {
	
	@Override
	public void createReportSkeleton_(ReportNode node, TestSet testSet) {
		runParallel(node, testSet, false);
	}

	@Override
	public void execute_(ReportNode node, TestSet testSet) {
		runParallel(node, testSet, true);
	}

	private void runParallel(ReportNode node, TestSet testSet, boolean execution) {
		int numberOfThreads = ExecutionContext.getCurrentContext().getVariablesManager().getVariableAsInteger("tec.execution.threads",1);

		TestSetScheduler scheduler = new DefaultTestSetScheduler();
		List<TestCaseBundle> bundles = scheduler.bundleTestCases(getChildren(testSet), numberOfThreads);
		ExecutorService executor = Executors.newFixedThreadPool(bundles.size());
		try {
			for(TestCaseBundle bundle:bundles) {
				BundleProcessor bundleProcessor = new BundleProcessor(testSet, bundle, node, execution);
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
			
			ReportNodeStatus status;
			if(context.isInterrupted()) {
				status = ReportNodeStatus.INTERRUPTED;
			} else {
				status = ReportNodeStatus.PASSED;
			}
			
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

		public BundleProcessor(TestSet testSet, TestCaseBundle bundle, ReportNode node, boolean execution) {
			super();
			this.bundle = bundle;
			this.node = node;
			this.execution = execution;
		}

		@Override
		public void run() {
			ExecutionContext.setCurrentContext(context);
			
			try {
				for(AbstractArtefact testArtefact:bundle.getTestcases()) {
					if(context.isInterrupted()) {
						break;
					}				
					if(execution) {
						delegateExecute(testArtefact, node);	
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
