package step.artefacts.handlers;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import step.artefacts.TestScenario;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;

public class TestScenarioHandler extends ArtefactHandler<TestScenario, ReportNode> {

	@Override
	public void createReportSkeleton_(ReportNode node, TestScenario testArtefact) {
		for(AbstractArtefact child:getChildren(testArtefact)) {
			delegateCreateReportSkeleton(child, node);
		}
	}

	@Override
	public void execute_(final ReportNode node, TestScenario testArtefact) {
		List<AbstractArtefact> artefacts = getChildren(testArtefact);
		ExecutorService executor = Executors.newFixedThreadPool(artefacts.size());
		for(final AbstractArtefact child:artefacts) {
			executor.submit(new Runnable() {
				public void run() {
					ExecutionContext.setCurrentContext(context);
					delegateExecute(child, node);
				}
			});
		}
		
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			logger.error("An error occcurred while waiting for the executor to terminate",e);
		}
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, TestScenario testArtefact) {
		return new ReportNode();
	}

}
