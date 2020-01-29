package step.core.execution;

import java.util.Map;

import org.bson.types.ObjectId;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.plans.Plan;
import step.core.plans.runner.PlanRunner;
import step.core.plans.runner.PlanRunnerResult;

/**
 * A runner that runs plans on a controller instance
 * 
 * @author Jérôme Comte
 *
 */
public class ControllerPlanRunner implements PlanRunner {

	private final ExecutionLifecycleManager executionLifecycleManager;
	private final ExecutionContext executionContext;

	public ControllerPlanRunner(ExecutionLifecycleManager executionLifecycleManager,
			ExecutionContext executionContext) {
		super();
		this.executionLifecycleManager = executionLifecycleManager;
		this.executionContext = executionContext;
	}

	@Override
	public PlanRunnerResult run(Plan plan) {
		executionContext.associateThread();
		
		executionContext.setPlan(plan);
		
		AbstractArtefact root = plan.getRoot();
		ReportNode rootReportNode = createAndPersistRootReportNode();
		
		executionLifecycleManager.executionStarted();
		
		ArtefactHandler.delegateCreateReportSkeleton(executionContext, root, rootReportNode);
		ReportNode planReportNode = ArtefactHandler.delegateExecute(executionContext, root, rootReportNode);
		
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


	private ReportNode createAndPersistRootReportNode() {
		ReportNode resultNode = new ReportNode();
		resultNode.setExecutionID(executionContext.getExecutionId());
		resultNode.setId(new ObjectId(executionContext.getExecutionId()));
		executionContext.setReport(resultNode);
		executionContext.getReportNodeCache().put(resultNode);
		executionContext.getReportNodeAccessor().save(resultNode);
		executionContext.setCurrentReportNode(resultNode);
		return resultNode;
	}
}
