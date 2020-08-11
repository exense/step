package step.core.execution;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandlerManager;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;
import step.core.execution.model.ReportExport;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plans.runner.PlanRunnerResult;
import step.core.repositories.ImportResult;
import step.core.repositories.RepositoryObjectManager;
import step.core.repositories.RepositoryObjectReference;
import step.engine.execution.ExecutionLifecycleManager;

public class ExecutionEngineRunner {

	private static final Logger logger = LoggerFactory.getLogger(ExecutionEngineRunner.class);
	protected final ExecutionContext executionContext;
	protected final ExecutionLifecycleManager executionLifecycleManager;
	protected final RepositoryObjectManager repositoryObjectManager;
	protected final ExecutionAccessor executionAccessor; 

	protected ExecutionEngineRunner(ExecutionContext executionContext) {
		super();
		this.executionContext = executionContext;
		this.executionLifecycleManager = new ExecutionLifecycleManager(executionContext);
		this.repositoryObjectManager = executionContext.getRepositoryObjectManager();
		this.executionAccessor = executionContext.getExecutionAccessor();
	}
	
	protected PlanRunnerResult execute() {
		String executionId = executionContext.getExecutionId();
		PlanRunnerResult result = result(executionId);
		try {
			ExecutionParameters executionParameters = executionContext.getExecutionParameters();
			Plan plan = executionParameters.getPlan();
			
			if(plan == null) {
				executionLifecycleManager.beforePlanImport();
				
				updateStatus(ExecutionStatus.IMPORTING);
				ImportResult importResult = importPlan(executionContext);
				
				executionLifecycleManager.afterImport(importResult);
				
				if(importResult.isSuccessful()) {
					PlanAccessor planAccessor = executionContext.getPlanAccessor();
					plan = planAccessor.get(new ObjectId(importResult.getPlanId()));
				}
			}
			
			if(plan != null) {
				executionContext.setPlan(plan);
				
				logger.info("Starting test execution. Execution ID: " + executionId);
				updateStatus(ExecutionStatus.RUNNING);
				
				executionContext.associateThread();
				
				ReportNode rootReportNode = executionContext.getReport();
				executionContext.setCurrentReportNode(rootReportNode);
				persistReportNode(rootReportNode);
				
				executionLifecycleManager.executionStarted();
				
				ReportNode planReportNode = execute(plan, rootReportNode);
				
				if(planReportNode!=null && planReportNode.getStatus() != null) {
					ReportNodeStatus resultStatus = planReportNode.getStatus();
					rootReportNode.setStatus(resultStatus);
					executionContext.getReportNodeAccessor().save(rootReportNode);
				}
				
				result.waitForExecutionToTerminate();
				ReportNodeStatus resultStatus = result.getResult();
				executionLifecycleManager.updateExecutionResult(executionContext, resultStatus);
				
				logger.debug("Test execution ended. Reporting result.... Execution ID: " + executionId);
				
				if(!executionContext.isSimulation()) {
					updateStatus(ExecutionStatus.EXPORTING);
					exportExecution(executionContext);				
					logger.info("Test execution ended and reported. Execution ID: " + executionId);
				} else {
					logger.info("Test execution simulation ended. Test report isn't reported in simulation mode. Execution ID: " + executionId);
				}				
			} else {
				updateStatus(ExecutionStatus.ENDED);
			}
		} catch (Throwable e) {
			logger.error("An error occurred while running test. Execution ID: " + executionId, e);
		} finally {
			updateStatus(ExecutionStatus.ENDED);
			executionLifecycleManager.executionEnded();
		}
		return result;
	}
	
	public PlanRunnerResult run(Plan plan) {
		executionContext.getPlanAccessor().save(plan);
		
		return result(executionContext.getExecutionId());
	}

	protected ReportNode execute(Plan plan, ReportNode rootReportNode) {
		ArtefactHandlerManager artefactHandlerManager = executionContext.getArtefactHandlerManager();
		AbstractArtefact root = plan.getRoot();
		artefactHandlerManager.createReportSkeleton(root, rootReportNode);
		ReportNode planReportNode = artefactHandlerManager.execute(root, rootReportNode);
		return planReportNode;
	}
	
	protected PlanRunnerResult result(String executionId) {
		return new PlanRunnerResult(executionId, executionContext.getReport().getId().toString(), executionContext.getReportNodeAccessor());
	}
	
	private ImportResult importPlan(ExecutionContext context) throws Exception {
		ImportResult importResult;
		RepositoryObjectReference repositoryObjectReference = context.getExecutionParameters().getRepositoryObject();
		if(repositoryObjectReference!=null) {
			try {
				importResult = repositoryObjectManager.importPlan(context, repositoryObjectReference);											
			} catch (Exception e) {
				logger.error("Error while importing repository object "+repositoryObjectReference.toString(), e);
				importResult = new ImportResult();
				String error = "Unexpected error while importing plan: "+e.getMessage();
				List<String> errors = new ArrayList<>();
				errors.add(error);
				importResult.setErrors(errors);
			}
		} else {
			// TODO
			importResult = null;
		}
		return importResult;
	}
	
	private void exportExecution(ExecutionContext context) {	
		String executionId = context.getExecutionId();
		Execution execution = executionAccessor.get(executionId);
		
		if(execution!=null) {
			ReportExport report = repositoryObjectManager.exportTestExecutionReport(context, execution.getExecutionParameters().getRepositoryObject());
			List<ReportExport> exports = new ArrayList<>();
			exports.add(report);
			
			execution.setReportExports(exports);
			executionAccessor.save(execution);
		} else {
			// TODO decide what to do with this error
			//throw new RuntimeException("Unable to find execution with id "+executionId);
		}
	}

	private void persistReportNode(ReportNode reportNode) {
		executionContext.getReportNodeAccessor().save(reportNode);
	}
	
	private void updateStatus(ExecutionStatus newStatus) {
		executionLifecycleManager.updateStatus(newStatus);
	}
}
