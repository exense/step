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

import javax.json.JsonObject;

import step.artefacts.handlers.CallFunctionHandler;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.reports.Error;
import step.core.reports.ErrorType;
import step.core.variables.VariableType;
import step.functions.handler.JsonBasedFunctionHandler;
import step.functions.io.Input;
import step.functions.io.Output;
import step.functions.io.OutputBuilder;

public class ArtefactFunctionHandler extends JsonBasedFunctionHandler {

	@Override
	protected Output<JsonObject> handle(Input<JsonObject> input) {
		ExecutionContext executionContext = (ExecutionContext) getToken().getToken().getAttachedObject(CallFunctionHandler.EXECUTION_CONTEXT_KEY);
		
		String artefactId = input.getProperties().get(CallFunctionHandler.ARTEFACTID);
		String parentReportId = input.getProperties().get(CallFunctionHandler.PARENTREPORTID);
		
		ReportNode parentNode;
		if(parentReportId!=null) {
			parentNode = executionContext.getReportNodeAccessor().get(parentReportId);
		} else {
			throw new RuntimeException("Parent node id is null. This should never occur");
		}
		
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
				Error error = new Error();
				error.setCode(0);
				error.setMsg("Error in composite keyword");
				error.setRoot(false);
				error.setType(node.getStatus().equals(ReportNodeStatus.FAILED)?ErrorType.BUSINESS:ErrorType.TECHNICAL);
				output.setError(error);
			}
			return output.build();
		} finally {
			executionContext.getVariablesManager().removeVariable(parentNode, "output");
			executionContext.setCurrentReportNode(previousCurrentNode);
		}
	}

}
