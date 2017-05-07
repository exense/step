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
import step.core.execution.model.ExecutionStatus;
import step.core.execution.model.ReportExport;
import step.core.repositories.RepositoryObjectManager;
import step.core.repositories.RepositoryObjectReference;

public class ExecutionRunnable implements Runnable {
	
	private  final static Logger logger = LoggerFactory.getLogger(ExecutionRunnable.class);

	final ExecutionContext context;
						
	public ExecutionRunnable(ExecutionContext context) {
		super();
		this.context = context;
	}

	public ExecutionContext getContext() {
		return context;
	}
	
	@Override
	public void run() {
		try {
			ExecutionContext.setCurrentContext(context);

			ReportNode rootReportNode = createAndPersistRootReportNode();

			context.getGlobalContext().getExecutionLifecycleManager().executionStarted(this);
			
			updateStatus(ExecutionStatus.IMPORTING);
			String artefactID = importArtefact();
						
			AbstractArtefact artefact = context.getGlobalContext().getArtefactAccessor().get(artefactID);
			context.setArtefact(artefact);
			
			logger.info("Starting test execution. Execution ID: " + context.getExecutionId());
			updateStatus(ExecutionStatus.RUNNING);
						
			ArtefactHandler.delegateCreateReportSkeleton(context, artefact, rootReportNode);
			ArtefactHandler.delegateExecute(context, artefact, rootReportNode);

			logger.debug("Test execution ended. Reporting result.... Execution ID: " + context.getExecutionId());

			if(!context.isSimulation()) {
				updateStatus(ExecutionStatus.EXPORTING);
				List<ReportExport> exports = exportExecution(context.getExecutionId());	
				context.setReportExports(exports);				
				logger.info("Test execution ended and reported. Execution ID: " + context.getExecutionId());
			} else {
				logger.info("Test execution simulation ended. Test report isn't reported in simulation mode. Execution ID: " + context.getExecutionId());
			}
			
		} catch (Throwable e) {
			logger.error("An error occurred while running test. Execution ID: " + context.getExecutionId(), e);
		} finally {
			updateStatus(ExecutionStatus.ENDED);
			context.getGlobalContext().getExecutionLifecycleManager().executionEnded(this);
		}
	}

	private ReportNode createAndPersistRootReportNode() {
		ReportNode resultNode = new ReportNode();
		resultNode.setExecutionID(context.getExecutionId());
		resultNode._id = new ObjectId(context.getExecutionId());
		context.setReport(resultNode);
		context.getReportNodeCache().put(resultNode);
		context.getGlobalContext().getReportAccessor().save(resultNode);
		ExecutionContext.setCurrentReportNode(resultNode);
		return resultNode;
	}
	
	private void updateStatus(ExecutionStatus newStatus) {
		context.getGlobalContext().getExecutionLifecycleManager().updateStatus(this, newStatus);
	}
	
	private String importArtefact() throws Exception {
		String artefactID;
		if(context.getExecutionParameters().getArtefact()!=null) {
			RepositoryObjectReference artefactPointer = context.getExecutionParameters().getArtefact();
			if(artefactPointer!=null) {
				if("local".equals(artefactPointer.getRepositoryID())) {
					artefactID = artefactPointer.getRepositoryParameters().get("artefactid");
				} else {
					artefactID = context.getGlobalContext().getRepositoryObjectManager().importArtefact(artefactPointer);					
				}
			} else {
				throw new Exception("context.artefactID is null and no ArtefactPointer has been specified. This shouldn't happen.");
			}
		} else {
			// TODO
			artefactID = null;
		}
		return artefactID;
	}
	
	private List<ReportExport> exportExecution(String executionId) {		
		Execution execution = context.getGlobalContext().getExecutionAccessor().get(executionId);
		
		if(execution!=null) {
			RepositoryObjectManager repositoryObjectManager = context.getGlobalContext().getRepositoryObjectManager();
			ReportExport report = repositoryObjectManager.exportTestExecutionReport(execution.getExecutionParameters().getArtefact(), executionId);
			List<ReportExport> exports = new ArrayList<>();
			exports.add(report);
			
			return exports;			
		} else {
			throw new RuntimeException("Unable to find execution with id "+executionId);
		}
		
	}

	@Override
	public boolean equals(Object obj) {
		return ((ExecutionRunnable)obj).getContext().getExecutionId().equals(getContext().getExecutionId());
	}
	
	public void cleanUp() {
	}
	
}