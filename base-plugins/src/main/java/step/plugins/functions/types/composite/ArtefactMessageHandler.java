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
package step.plugins.functions.types.composite;

import org.bson.types.ObjectId;

import step.artefacts.handlers.CallFunctionHandler;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.variables.VariableType;
import step.functions.handler.AbstractFunctionHandler;
import step.functions.io.Input;
import step.functions.io.Output;
import step.functions.io.OutputBuilder;

public class ArtefactMessageHandler extends AbstractFunctionHandler {

	@Override
	protected Output<?> handle(Input<?> input) {
		ExecutionContext executionContext = (ExecutionContext) getToken().getToken().getAttachedObject(CallFunctionHandler.EXECUTION_CONTEXT_KEY);
		
		String artefactId = input.getProperties().get(CallFunctionHandler.ARTEFACTID);
		String parentReportId = input.getProperties().get(CallFunctionHandler.PARENTREPORTID);
		
		ReportNode parentNode;
		parentNode = new ReportNode();
		if(parentReportId!=null) {
			parentNode.setParentID(new ObjectId(parentReportId));
		}
		parentNode.setExecutionID(executionContext.getExecutionId());
		
		executionContext.getReportNodeAccessor().save(parentNode);

		ReportNode previousCurrentNode = executionContext.getCurrentReportNode();
		executionContext.setCurrentReportNode(parentNode);
		executionContext.getReportNodeCache().put(parentNode);
		
		AbstractArtefact artefact = executionContext.getArtefactAccessor().get(artefactId);
		
		executionContext.getVariablesManager().putVariable(parentNode, "input", input.getPayload());
		OutputBuilder output = new OutputBuilder();
		executionContext.getVariablesManager().putVariable(parentNode, VariableType.IMMUTABLE, "output", output);
		
		try {
			ReportNode node = ArtefactHandler.delegateExecute(executionContext, artefact,parentNode);
			if(node.getStatus()== ReportNodeStatus.TECHNICAL_ERROR || node.getStatus()== ReportNodeStatus.FAILED) {
				output.setError("Error in composite execution. Composite status: " + node.getStatus() + 
						(node.getError()!=null?". Error message: "+node.getError().getMsg():""));						
			}
			return output.build();
		} finally {
			executionContext.getVariablesManager().removeVariable(parentNode, "output");
			executionContext.setCurrentReportNode(previousCurrentNode);
		}
	}

}
