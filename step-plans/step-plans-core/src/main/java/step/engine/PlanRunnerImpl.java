package step.engine;

import java.util.Map;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandlerManager;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.plans.Plan;
import step.core.plans.runner.PlanRunner;
import step.core.plans.runner.PlanRunnerResult;
import step.engine.execution.ExecutionLifecycleManager;

/**
 * A runner that runs plans on a controller instance
 * 
 * @author Jérôme Comte
 *
 */
public class PlanRunnerImpl implements PlanRunner {

	private final ExecutionContext executionContext;
	private final ExecutionLifecycleManager executionLifecycleManager;

	public PlanRunnerImpl(ExecutionContext executionContext) {
		super();
		this.executionContext = executionContext;
		this.executionLifecycleManager = new ExecutionLifecycleManager(executionContext);
	}

	@Override
	public PlanRunnerResult run(Plan plan) {
		executionContext.associateThread();
		
		ReportNode rootReportNode = executionContext.getReport();
		executionContext.setCurrentReportNode(rootReportNode);
		persistReportNode(rootReportNode);
		
		executionLifecycleManager.executionStarted();
		
		ArtefactHandlerManager artefactHandlerManager = executionContext.getArtefactHandlerManager();
		
		AbstractArtefact root = plan.getRoot();
		artefactHandlerManager.createReportSkeleton(root, rootReportNode);
		ReportNode planReportNode = artefactHandlerManager.execute(root, rootReportNode);
		
		if(planReportNode!=null && planReportNode.getStatus() != null) {
			ReportNodeStatus resultStatus = planReportNode.getStatus();
			rootReportNode.setStatus(resultStatus);
			executionContext.getReportNodeAccessor().save(rootReportNode);
		}
		
		return new PlanRunnerResult(executionContext.getExecutionId(), rootReportNode.getId().toString(), executionContext.getReportNodeAccessor());
	}

	@Override
	public PlanRunnerResult run(Plan plan, Map<String, String> executionParameters) {
		throw new UnsupportedOperationException("Running a plan with execution parameters isn't support by this runner.");
	}

	private void persistReportNode(ReportNode reportNode) {
		executionContext.getReportNodeAccessor().save(reportNode);
	}
}
