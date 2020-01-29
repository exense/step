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
package step.core.execution;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.ExecutionStatus;
import step.core.execution.model.ReportExport;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.core.plans.runner.PlanRunner;
import step.core.plans.runner.PlanRunnerResult;
import step.core.repositories.ImportResult;
import step.core.repositories.RepositoryObjectManager;
import step.core.repositories.RepositoryObjectReference;

public class ExecutionRunnable implements Runnable {
	
	private  final static Logger logger = LoggerFactory.getLogger(ExecutionRunnable.class);

	final RepositoryObjectManager repositoryObjectManager;
	final ExecutionAccessor executionAccessor; 
	
	final ExecutionContext context;
	
	final ExecutionLifecycleManager executionLifecycleManager;
						
	public ExecutionRunnable(RepositoryObjectManager repositoryObjectManager, ExecutionAccessor executionAccessor, ExecutionContext context) {
		super();
		this.repositoryObjectManager = repositoryObjectManager;
		this.executionAccessor = executionAccessor;
		this.context = context;
		this.executionLifecycleManager = new ExecutionLifecycleManager(context.get(ExecutionManager.class), context);
	}

	public ExecutionContext getContext() {
		return context;
	}
	
	@Override
	public void run() {
		try {
			updateStatus(ExecutionStatus.IMPORTING);
			ImportResult importResult = importPlan();
			
			executionLifecycleManager.afterImport(importResult);
			
			if(importResult.isSuccessful()) {
				PlanAccessor planAccessor = context.getPlanAccessor();
				Plan plan = planAccessor.get(new ObjectId(importResult.getPlanId()));
				
				logger.info("Starting test execution. Execution ID: " + context.getExecutionId());
				updateStatus(ExecutionStatus.RUNNING);
				
				PlanRunner planRunner = new ControllerPlanRunner(executionLifecycleManager, context);
				PlanRunnerResult result = planRunner.run(plan);
				
				result.waitForExecutionToTerminate();
				ReportNodeStatus resultStatus = result.getResult();
				executionLifecycleManager.updateExecutionResult(context, resultStatus);
				
				logger.debug("Test execution ended. Reporting result.... Execution ID: " + context.getExecutionId());
				
				if(!context.isSimulation()) {
					updateStatus(ExecutionStatus.EXPORTING);
					exportExecution(context.getExecutionId());				
					logger.info("Test execution ended and reported. Execution ID: " + context.getExecutionId());
				} else {
					logger.info("Test execution simulation ended. Test report isn't reported in simulation mode. Execution ID: " + context.getExecutionId());
				}				
			} else {
				updateStatus(ExecutionStatus.ENDED);
			}
						
			
		} catch (Throwable e) {
			logger.error("An error occurred while running test. Execution ID: " + context.getExecutionId(), e);
		} finally {
			updateStatus(ExecutionStatus.ENDED);
			executionLifecycleManager.executionEnded();
		}
	}
	
	private void updateStatus(ExecutionStatus newStatus) {
		executionLifecycleManager.updateStatus(newStatus);
	}
	
	private ImportResult importPlan() throws Exception {
		ImportResult importResult;
		RepositoryObjectReference repositoryObjectReference = context.getExecutionParameters().getRepositoryObject();
		if(repositoryObjectReference!=null) {
			if("local".equals(repositoryObjectReference.getRepositoryID())) {
				importResult = new ImportResult();
				importResult.setPlanId(repositoryObjectReference.getRepositoryParameters().get(RepositoryObjectReference.PLAN_ID));
				importResult.setSuccessful(true);
			} else {
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
			}
		} else {
			// TODO
			importResult = null;
		}
		return importResult;
	}
	
	private void exportExecution(String executionId) {		
		Execution execution = executionAccessor.get(executionId);
		
		if(execution!=null) {
			ReportExport report = repositoryObjectManager.exportTestExecutionReport(context, execution.getExecutionParameters().getRepositoryObject());
			List<ReportExport> exports = new ArrayList<>();
			exports.add(report);
			
			execution.setReportExports(exports);
			executionAccessor.save(execution);
		} else {
			throw new RuntimeException("Unable to find execution with id "+executionId);
		}
		
	}
	
	public ExecutionLifecycleManager getExecutionLifecycleManager() {
		return executionLifecycleManager;
	}

	@Override
	public boolean equals(Object obj) {
		return ((ExecutionRunnable)obj).getContext().getExecutionId().equals(getContext().getExecutionId());
	}
	
	public void cleanUp() {
	}
	
}