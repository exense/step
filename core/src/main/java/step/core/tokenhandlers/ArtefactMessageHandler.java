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
package step.core.tokenhandlers;

import org.bson.types.ObjectId;

import step.core.GlobalContext;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.variables.VariableType;
import step.grid.agent.handler.MessageHandler;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class ArtefactMessageHandler implements MessageHandler {

	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message)
			throws Exception {
		ExecutionContext executionContext = ExecutionContext.getCurrentContext();
		GlobalContext globalContext = executionContext.getGlobalContext();
		
		String artefactId = message.getProperties().get("artefactid");
		String parentReportId = message.getProperties().get("parentreportid");
		
		ReportNode parentNode;
		parentNode = new ReportNode();
		if(parentReportId!=null) {
			parentNode.setParentID(new ObjectId(parentReportId));
		}
		parentNode.setExecutionID(executionContext.getExecutionId());
		
		globalContext.getReportAccessor().save(parentNode);

		ReportNode previousCurrentNode = ExecutionContext.getCurrentReportNode();
		ExecutionContext.setCurrentReportNode(parentNode);
		executionContext.getReportNodeCache().put(parentNode);
		
		AbstractArtefact artefact = globalContext.getArtefactAccessor().get(artefactId);
		
		executionContext.getVariablesManager().putVariable(parentNode, "args", message.getArgument());
		OutputMessage output = new OutputMessage();
		executionContext.getVariablesManager().putVariable(parentNode, VariableType.IMMUTABLE, "output", output);
		
		try {
			ReportNode node = ArtefactHandler.delegateExecute(artefact,parentNode);
			if(node.getStatus()== ReportNodeStatus.TECHNICAL_ERROR || node.getStatus()== ReportNodeStatus.FAILED) {
				output.setError("Error in composite execution. Composite status: " + node.getStatus() + 
						(node.getError()!=null?". Error message: "+node.getError().getMsg():""));						
			}
			return output;
		} finally {
			executionContext.getVariablesManager().removeVariable(parentNode, "output");
			ExecutionContext.setCurrentReportNode(previousCurrentNode);
		}
		
	}

}
