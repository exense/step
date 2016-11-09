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
package step.artefacts.handlers.scheduler;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.miscellaneous.TestArtefactResultHandler;
import step.core.variables.UndefinedVariableException;

public class SequentialArtefactScheduler {

	public SequentialArtefactScheduler() {
		super();
	}

	public void createReportSkeleton_(ReportNode node, AbstractArtefact testArtefact) {		
		for(AbstractArtefact child:ArtefactHandler.getChildren(testArtefact)) {
			ArtefactHandler.delegateCreateReportSkeleton(child, node);
		}
	}
	
	public void execute_(ReportNode node, AbstractArtefact testArtefact) {
		ExecutionContext context = ExecutionContext.getCurrentContext();
		ReportNodeStatus parentResultStatus = node.getStatus(); 
		try {			
			for(AbstractArtefact child:ArtefactHandler.getChildren(testArtefact)) {
				if(context.isInterrupted()) {
					break;
				}
				ReportNode resultNode = ArtefactHandler.delegateExecute(child, node);
				
				if(parentResultStatus==null || resultNode.getStatus().ordinal()<parentResultStatus.ordinal()) {
					parentResultStatus = resultNode.getStatus();
				}
				
				Boolean continueOnce = null;
				try {
					continueOnce = context.getVariablesManager().getVariableAsBoolean(ArtefactHandler.CONTINUE_EXECUTION_ONCE);
				} catch (UndefinedVariableException e) {} finally {
					context.getVariablesManager().removeVariable(node, ArtefactHandler.CONTINUE_EXECUTION_ONCE);												
				}
				
				if(resultNode.getStatus()==ReportNodeStatus.TECHNICAL_ERROR || resultNode.getStatus()==ReportNodeStatus.FAILED) {
					if(!context.isSimulation()) {
						if(continueOnce!=null) {
							if(!continueOnce) {
								break;
							}
						} else {
							if (!context.getVariablesManager().getVariableAsBoolean(ArtefactHandler.CONTINUE_EXECUTION, false)) {
								break;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			TestArtefactResultHandler.failWithException(node, e);
		} finally {
			Object forcedStatus = context.getVariablesManager().getVariable("tec.forceparentstatus");
			if(forcedStatus!=null) {
				node.setStatus(ReportNodeStatus.valueOf(forcedStatus.toString()));
			} else {
				if(context.isInterrupted()) {
					node.setStatus(ReportNodeStatus.INTERRUPTED);
				} else {
					node.setStatus(parentResultStatus);					
				}
			}
		}
	}

}
