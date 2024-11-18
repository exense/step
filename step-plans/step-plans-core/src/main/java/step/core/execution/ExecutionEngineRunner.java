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

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandlerManager;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ParentSource;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.artefacts.reports.aggregated.ReportNodeTimeSeries;
import step.core.artefacts.reports.resolvedplan.ResolvedPlanBuilder;
import step.core.artefacts.reports.resolvedplan.ResolvedPlanNode;
import step.core.execution.model.*;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plans.runner.PlanRunnerResult;
import step.core.plugins.ExecutionCallbacks;
import step.core.reports.Error;
import step.core.reports.ErrorType;
import step.core.repositories.ImportResult;
import step.core.repositories.RepositoryObjectManager;
import step.core.repositories.RepositoryObjectReference;
import step.engine.execution.ExecutionManager;
import step.engine.execution.ExecutionVeto;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ExecutionEngineRunner {

	private static final Logger logger = LoggerFactory.getLogger(ExecutionEngineRunner.class);
	public static final String EXECUTION_ENGINE_LAYER = "executionEngine";
	private final ExecutionContext executionContext;
	private final ExecutionCallbacks executionCallbacks;
	private final ExecutionManager executionManager;
	private final RepositoryObjectManager repositoryObjectManager;
	private final PlanAccessor planAccessor;
	private final FunctionAccessor functionAccessor;
	// Temporary switch to disable the aggregated report. Remove as soon as SED-3464 is fixed
	private final boolean aggregatedReportEnabled;

	protected ExecutionEngineRunner(ExecutionContext executionContext) {
		super();
		this.executionContext = executionContext;
		this.executionManager = executionContext.getExecutionManager();
		this.executionCallbacks = executionContext.getExecutionCallbacks();
		this.repositoryObjectManager = executionContext.getRepositoryObjectManager();
		this.planAccessor = executionContext.getPlanAccessor();
		this.functionAccessor =  executionContext.get(FunctionAccessor.class);
		aggregatedReportEnabled = executionContext.getConfiguration().getPropertyAsBoolean("execution.engine.report.aggregated.enabled", true);
	}
	
	protected PlanRunnerResult execute() {
		String executionId = executionContext.getExecutionId();
		PlanRunnerResult result = result(executionId);
		try {
			List<ExecutionVeto> vetoes = getExecutionVetoes();
			if (!vetoes.isEmpty()) {
				logger.info(messageWithId("Execution was vetoed."));
				ImportResult importResult = new ImportResult();
				importResult.setSuccessful(false);
				importResult.setErrors(vetoes.stream().map(v -> v.reason).collect(Collectors.toList()));
				addImportResultToExecution(importResult);
				saveFailureReportWithResult(ReportNodeStatus.VETOED);
			} else {
				try {
					Plan plan = getPlanFromExecutionParametersOrImport();
					addPlanToContextAndUpdateExecution(plan);

					logger.info(messageWithId("Starting execution."));
					updateStatus(ExecutionStatus.ESTIMATING);

					//resolve and save any embedded functions and sub plans before resolving the plan
					resolveInnerPlanAndFunctionArtefacts(plan);
					buildAndPersistResolvedPlan(plan);

					executionContext.associateThread();

					ReportNode rootReportNode = executionContext.getReport();
					executionContext.setCurrentReportNode(rootReportNode);
					persistReportNode(rootReportNode);

					executionCallbacks.executionStart(executionContext);
					ReportNode planReportNode = execute(plan, rootReportNode);

					if (planReportNode != null && planReportNode.getStatus() != null) {
						ReportNodeStatus resultStatus = planReportNode.getStatus();
						rootReportNode.setStatus(resultStatus);
						persistReportNode(rootReportNode);
						updateExecutionResult(resultStatus);
					}

					if(!executionContext.isSimulation()) {
						logger.debug(messageWithId("Execution ended. Exporting report...."));
						updateStatus(ExecutionStatus.EXPORTING);
						exportExecution(executionContext);
						logger.info(messageWithId("Execution report exported."));
					} else {
						logger.info(messageWithId("Execution simulation ended. Skipping report export in simulation mode."));
					}
				} catch (PlanImportException e) {
					saveFailureReportWithResult(ReportNodeStatus.IMPORT_ERROR);
				} catch (ProvisioningException e) {
					addLifecyleError(e.getMessage(), e);
				} catch (DeprovisioningException e) {
					addLifecyleError(e.getMessage(), e);
				}
			}
		} catch (Throwable e) {
			addLifecyleError("An error occurred while running test. " +  e.getMessage(), e);
		} finally {
			try {
				executionCallbacks.afterExecutionEnd(executionContext);
			} catch (Exception e) {
				addLifecyleError(e.getMessage(), e);
			} finally {
				updateStatus(ExecutionStatus.ENDED);
				//Make sure that even if plugin critical exception occurs the mandatory hooks and postExecutions are performed
				executionCallbacks.executionFinally(executionContext);
				postExecution(executionContext);
			}
		}
		return result;
	}

	private void buildAndPersistResolvedPlan(Plan plan) {
		if(aggregatedReportEnabled) {
			ResolvedPlanBuilder resolvedPlanBuilder = new ResolvedPlanBuilder(executionContext);
			ResolvedPlanNode resolvedPlanRoot = resolvedPlanBuilder.buildResolvedPlan(plan);
			updateExecution(e -> e.setResolvedPlanRootNodeId(resolvedPlanRoot.getId().toString()));
		}
	}

	public static void abort(ExecutionContext executionContext) {
		if(executionContext.getStatus()!=ExecutionStatus.ENDED) {
			updateStatus(executionContext, ExecutionStatus.ABORTING);
		}
		executionContext.getExecutionCallbacks().beforeExecutionEnd(executionContext);
	}

	public static void forceAbort(ExecutionContext executionContext) {
		if(executionContext.getStatus()!=ExecutionStatus.ENDED) {
			updateStatus(executionContext, ExecutionStatus.FORCING_ABORT);
		}
		executionContext.getExecutionCallbacks().forceStopExecution(executionContext);
	}

	private List<ExecutionVeto> getExecutionVetoes() {
		return executionContext.getExecutionVetoers().stream()
				.map(v -> v.getExecutionVetoes(executionContext))
				.filter(Objects::nonNull).flatMap(List::stream)
				.collect(Collectors.toList());
	}

	private Plan getPlanFromExecutionParametersOrImport() throws PlanImportException {
		ExecutionParameters executionParameters = executionContext.getExecutionParameters();
		Plan executionParametersPlan = executionParameters.getPlan();

		if (executionParametersPlan == null) {
			ImportResult importResult = importPlan(executionContext);
			addImportResultToExecution(importResult);

			if (importResult.isSuccessful()) {
				PlanAccessor planAccessor = executionContext.getPlanAccessor();
				return planAccessor.get(new ObjectId(importResult.getPlanId()));
			} else {
				throw new PlanImportException();
			}
		} else {
			return executionParametersPlan;
		}
	}

	private void addImportResultToExecution(ImportResult importResult) {
		updateExecution(e -> e.setImportResult(importResult));
	}

	private void addPlanToContextAndUpdateExecution(Plan plan) {
		executionContext.setPlan(plan);
		updateExecution(execution -> {
			execution.setPlanId(plan.getId().toString());
			if (execution.getDescription() == null) {
				execution.setDescription(plan.getAttributes() != null ? plan.getAttributes().get(AbstractOrganizableObject.NAME) : null);
			}
		});
	}

	private String messageWithId(String message) {
		return message + " Execution ID: " + executionContext.getExecutionId();
	}

	private void resolveInnerPlanAndFunctionArtefacts(Plan plan) {
		// Save plan embedded functions to context accessor
		Collection<Function> planInnerFunctions = plan.getFunctions();
		if(planInnerFunctions!=null && planInnerFunctions.size()>0) {
			if(functionAccessor != null) {
				planInnerFunctions.forEach(f -> this.executionContext.getObjectEnricher().accept(f));
				functionAccessor.save(planInnerFunctions);
			} else {
				throw new RuntimeException("Unable to save inner functions because no function accessor is available");
			}
		}
		// Save sub plans to context accessor
		Collection<Plan> subPlans = plan.getSubPlans();
		if(subPlans!=null && subPlans.size()>0) {
			planAccessor.save(subPlans);
		}
	}

	private ReportNode execute(Plan plan, ReportNode rootReportNode) throws ProvisioningException, DeprovisioningException {
		ArtefactHandlerManager artefactHandlerManager = executionContext.getArtefactHandlerManager();
		AbstractArtefact root = plan.getRoot();
		artefactHandlerManager.createReportSkeleton(root, rootReportNode, ParentSource.MAIN);

		// Provision the resources required for the execution before starting the execution phase
		provisionRequiredResources();
		try {
			updateStatus(ExecutionStatus.RUNNING);
			return artefactHandlerManager.execute(root, rootReportNode, ParentSource.MAIN);
		} finally {
			try {
				//Flush report node TS
				executionContext.require(ReportNodeTimeSeries.class).flush();
			} catch (Exception e) {
				logger.error("Unable to flush report nodes time series upon execution end.", e);
			}
			// Deprovision the resources provisioned for the execution
			deprovisionRequiredResources();
		}
	}
	
	private PlanRunnerResult result(String executionId) {
		return new PlanRunnerResult(executionId, executionContext.getExecutionAccessor(), executionContext.getReportNodeAccessor(),
				executionContext.getResourceManager());
	}
	
	private ImportResult importPlan(ExecutionContext context) {
		executionCallbacks.beforePlanImport(context);
		updateStatus(ExecutionStatus.IMPORTING);
		ImportResult importResult;
		RepositoryObjectReference repositoryObjectReference = context.getExecutionParameters().getRepositoryObject();
		if(repositoryObjectReference!=null) {
			try {
				importResult = repositoryObjectManager.importPlan(context, repositoryObjectReference);											
			} catch (Exception e) {
				logger.error("Error while importing repository object "+repositoryObjectReference, e);
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

	private void provisionRequiredResources() throws ProvisioningException {
		updateStatus(ExecutionStatus.PROVISIONING);
		executionCallbacks.provisionRequiredResources(executionContext);
	}

	private void deprovisionRequiredResources() throws DeprovisioningException {
		updateStatus(ExecutionStatus.DEPROVISIONING);
		executionCallbacks.deprovisionRequiredResources(executionContext);
	}
	
	private void exportExecution(ExecutionContext context) {	
		updateExecution(execution -> {
			ReportExport report = repositoryObjectManager.exportTestExecutionReport(context, execution.getExecutionParameters().getRepositoryObject());
			List<ReportExport> exports = new ArrayList<>();
			exports.add(report);
			execution.setReportExports(exports);
		});
	}

	private void postExecution(ExecutionContext context) {
		Execution execution = executionManager.getExecution();
		Optional<RepositoryObjectReference> repositoryObjectReference = Optional.ofNullable(execution).map(Execution::getExecutionParameters).map(ExecutionParameters::getRepositoryObject);
		repositoryObjectReference.ifPresent(objectReference -> repositoryObjectManager.postExecution(context, objectReference));
	}

	private void persistReportNode(ReportNode rootReportNode) {
		executionContext.getReportNodeAccessor().save(rootReportNode);
	}

	private void updateExecutionResult(ReportNodeStatus resultStatus) {
		updateExecution(e -> e.setResult(resultStatus));
	}

	private void updateStatus(ExecutionStatus newStatus) {
		executionManager.updateStatus(newStatus);
	}

	private static void updateStatus(ExecutionContext executionContext, ExecutionStatus newStatus) {
		executionContext.getExecutionManager().updateStatus(newStatus);
	}

	private void saveFailureReportWithResult(ReportNodeStatus status) {
		ReportNode report = executionContext.getReport();
		report.setStatus(status);
		persistReportNode(report);
		updateExecutionResult(status);
	}

	private void addLifecyleError(String message, Throwable exception) {
		logger.error(messageWithId(message), exception);
		Error error = new Error(ErrorType.TECHNICAL, EXECUTION_ENGINE_LAYER, message, 0, true);
		updateExecution(e -> {
			e.addLifecyleError(error);
			e.setResult(ReportNodeStatus.TECHNICAL_ERROR);
		});
	}

	private void updateExecution(Consumer<Execution> consumer) {
		executionManager.updateExecution(consumer);
	}

	private class PlanImportException extends Exception {
	}

}
