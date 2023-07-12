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
package step.core.execution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
import step.engine.execution.ExecutionVeto;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;

public class ExecutionEngineRunner {

	private static final Logger logger = LoggerFactory.getLogger(ExecutionEngineRunner.class);
	protected final ExecutionContext executionContext;
	protected final ExecutionLifecycleManager executionLifecycleManager;
	protected final RepositoryObjectManager repositoryObjectManager;
	protected final PlanAccessor planAccessor; 
	protected final FunctionAccessor functionAccessor;
	protected final ExecutionAccessor executionAccessor;

	protected ExecutionEngineRunner(ExecutionContext executionContext) {
		super();
		this.executionContext = executionContext;
		this.executionLifecycleManager = new ExecutionLifecycleManager(executionContext);
		this.repositoryObjectManager = executionContext.getRepositoryObjectManager();
		this.planAccessor = executionContext.getPlanAccessor();
		this.functionAccessor =  executionContext.get(FunctionAccessor.class);
		this.executionAccessor = executionContext.getExecutionAccessor();
	}
	
	protected PlanRunnerResult execute() {
		String executionId = executionContext.getExecutionId();
		PlanRunnerResult result = result(executionId);
		try {
			Plan plan = null;
			List<ExecutionVeto> vetoes = executionLifecycleManager.getExecutionVetoes();
			if (!vetoes.isEmpty()) {
				logger.info("Execution {} was vetoed.", executionContext.getExecutionId());
				ImportResult importResult = new ImportResult();
				importResult.setSuccessful(false);
				importResult.setErrors(vetoes.stream().map(v -> v.reason).collect(Collectors.toList()));
				executionLifecycleManager.afterImport(importResult);
			} else {
				ExecutionParameters executionParameters = executionContext.getExecutionParameters();
				plan = executionParameters.getPlan();

				if (plan == null) {
					executionLifecycleManager.beforePlanImport();

					updateStatus(ExecutionStatus.IMPORTING);
					ImportResult importResult = importPlan(executionContext);

					executionLifecycleManager.afterImport(importResult);

					if (importResult.isSuccessful()) {
						PlanAccessor planAccessor = executionContext.getPlanAccessor();
						plan = planAccessor.get(new ObjectId(importResult.getPlanId()));
					}

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

	protected ReportNode execute(Plan plan, ReportNode rootReportNode) {
		Collection<Function> planInnerFunctions = plan.getFunctions();
		if(planInnerFunctions!=null && planInnerFunctions.size()>0) {
			if(functionAccessor != null) {
				planInnerFunctions.forEach(f -> this.executionContext.getObjectEnricher().accept(f));
				functionAccessor.save(planInnerFunctions);
			} else {
				throw new RuntimeException("Unable to save inner functions because no function accessor is available");
			}
		}
		Collection<Plan> subPlans = plan.getSubPlans();
		if(subPlans!=null && subPlans.size()>0) {
			planAccessor.save(subPlans);
		}
		
		ArtefactHandlerManager artefactHandlerManager = executionContext.getArtefactHandlerManager();
		AbstractArtefact root = plan.getRoot();
		artefactHandlerManager.createReportSkeleton(root, rootReportNode);
		ReportNode planReportNode = artefactHandlerManager.execute(root, rootReportNode);
		return planReportNode;
	}
	
	protected PlanRunnerResult result(String executionId) {
		return new PlanRunnerResult(executionId, executionContext.getReport().getId().toString(), executionContext.getReportNodeAccessor(),
				executionContext.getResourceManager());
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
