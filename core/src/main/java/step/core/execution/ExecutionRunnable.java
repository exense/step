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

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.ExecutionStatus;
import step.core.execution.model.ReportExport;
import step.core.repositories.Repository.ImportResult;
import step.core.repositories.RepositoryObjectManager;
import step.core.repositories.RepositoryObjectReference;

public class ExecutionRunnable implements Runnable {
	
	private  final static Logger logger = LoggerFactory.getLogger(ExecutionRunnable.class);

	final ExecutionContext context;
	
	final ExecutionLifecycleManager executionLifecycleManager;
						
	public ExecutionRunnable(ExecutionContext context) {
		super();
		this.context = context;
		this.executionLifecycleManager = new ExecutionLifecycleManager(context);
	}

	public ExecutionContext getContext() {
		return context;
	}
	
	@Override
	public void run() {
		try {
			context.associateThread();

			ReportNode rootReportNode = createAndPersistRootReportNode();

			executionLifecycleManager.executionStarted();
			
			updateStatus(ExecutionStatus.IMPORTING);
			ImportResult importResult = importArtefact();
			
			executionLifecycleManager.afterImport(importResult);
			
			if(importResult.isSuccessful()) {
				AbstractArtefact artefact = context.getGlobalContext().getArtefactAccessor().get(importResult.getArtefactId());
				context.setArtefact(artefact);
				
				logger.info("Starting test execution. Execution ID: " + context.getExecutionId());
				updateStatus(ExecutionStatus.RUNNING);
				
				ArtefactHandler.delegateCreateReportSkeleton(context, artefact, rootReportNode);
				ArtefactHandler.delegateExecute(context, artefact, rootReportNode);
				
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

	private ReportNode createAndPersistRootReportNode() {
		ReportNode resultNode = new ReportNode();
		resultNode.setExecutionID(context.getExecutionId());
		resultNode.setId(new ObjectId(context.getExecutionId()));
		context.setReport(resultNode);
		context.getReportNodeCache().put(resultNode);
		context.getGlobalContext().getReportAccessor().save(resultNode);
		context.setCurrentReportNode(resultNode);
		return resultNode;
	}
	
	private void updateStatus(ExecutionStatus newStatus) {
		executionLifecycleManager.updateStatus(newStatus);
	}
	
	private ImportResult importArtefact() throws Exception {
		ImportResult importResult;
		if(context.getExecutionParameters().getArtefact()!=null) {
			RepositoryObjectReference artefactPointer = context.getExecutionParameters().getArtefact();
			if(artefactPointer!=null) {
				if("local".equals(artefactPointer.getRepositoryID())) {
					importResult = new ImportResult();
					importResult.setArtefactId(artefactPointer.getRepositoryParameters().get("artefactid"));
					importResult.setSuccessful(true);
				} else {
					try {
						importResult = context.getGlobalContext().getRepositoryObjectManager().importArtefact(artefactPointer);											
					} catch (Exception e) {
						logger.error("Error while importing repository object "+artefactPointer.toString(), e);
						importResult = new ImportResult();
						String error = "Unexpected error while importing plan: "+e.getMessage();
						List<String> errors = new ArrayList<>();
						errors.add(error);
						importResult.setErrors(errors);
					}
				}
			} else {
				throw new Exception("context.artefactID is null and no ArtefactPointer has been specified. This shouldn't happen.");
			}
		} else {
			// TODO
			importResult = null;
		}
		return importResult;
	}
	
	private void exportExecution(String executionId) {		
		ExecutionAccessor executionAccessor = context.getGlobalContext().getExecutionAccessor();
		Execution execution = executionAccessor.get(executionId);
		
		if(execution!=null) {
			RepositoryObjectManager repositoryObjectManager = context.getGlobalContext().getRepositoryObjectManager();
			ReportExport report = repositoryObjectManager.exportTestExecutionReport(execution.getExecutionParameters().getArtefact(), executionId);
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