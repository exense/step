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
package step.plugins.functions.types.composite;

import javax.json.JsonObject;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.reports.Error;
import step.core.reports.ErrorType;
import step.core.variables.VariableType;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.handler.AbstractFunctionHandler;
import step.functions.handler.JsonBasedFunctionHandler;
import step.functions.io.Input;
import step.functions.io.Output;
import step.functions.io.OutputBuilder;

public class ArtefactFunctionHandler extends JsonBasedFunctionHandler {

	private static final String INPUT = "input";
	private static final String OUTPUT = "output";

	// Key agreed upon to pass the artefact serving as root to the handler (via props)
	public static final String COMPOSITE_FUNCTION_KEY = "$compfuncid";
	
	@Override
	protected Output<JsonObject> handle(Input<JsonObject> input) {
		OutputBuilder output = new OutputBuilder();

		ExecutionContext executionContext = (ExecutionContext) getTokenReservationSession().get(AbstractFunctionHandler.EXECUTION_CONTEXT_KEY);
		
		if(executionContext == null) {
			output.setError("Running composite Keyword on agent not supported. Please change the keyword configuration accordingly.");
		} else {
			String compFunctionId = input.getProperties().get(COMPOSITE_FUNCTION_KEY);
			String parentReportId = input.getProperties().get(AbstractFunctionHandler.PARENTREPORTID_KEY);
			
			ReportNode parentNode;
			if(parentReportId!=null) {
				parentNode = executionContext.getReportNodeAccessor().get(parentReportId);
				if(parentNode == null) {
					parentNode = executionContext.getCurrentReportNode();
				}
			} else {
				parentNode = executionContext.getCurrentReportNode();
				//throw new RuntimeException("Parent node id is null. This should never occur");
			}
			
			ReportNode previousCurrentNode = executionContext.getCurrentReportNode();
			executionContext.setCurrentReportNode(parentNode);
			executionContext.getReportNodeCache().put(parentNode);

			FunctionAccessor functionAccessor = executionContext.get(FunctionAccessor.class);
			if (functionAccessor == null) {
				// function access has to be properly initialized via step.core.execution.ExecutionContextWrapper to handle a composite function
				throw new IllegalStateException("Function accessor is not initialized within the execution context");
			}
			Function function = functionAccessor.get(compFunctionId);
			if (!(function instanceof ArtefactFunction)) {
				throw new IllegalStateException("Function " + compFunctionId + " has no linked plan");
			}

			AbstractArtefact artefact = ((ArtefactFunction) function).getPlan().getRoot();
			
			executionContext.getVariablesManager().putVariable(parentNode, INPUT, input.getPayload());
			executionContext.getVariablesManager().putVariable(parentNode, VariableType.IMMUTABLE, OUTPUT, output);
			
			try {
				ReportNode node = executionContext.getArtefactHandlerManager().execute(artefact, parentNode);
				if(node.getStatus()== ReportNodeStatus.TECHNICAL_ERROR || node.getStatus()== ReportNodeStatus.FAILED) {
					Error error = new Error();
					error.setCode(0);
					error.setMsg("Error in composite keyword");
					error.setRoot(false);
					error.setType(node.getStatus().equals(ReportNodeStatus.FAILED)?ErrorType.BUSINESS:ErrorType.TECHNICAL);
					output.setError(error);
				}
			} finally {
				executionContext.getVariablesManager().removeVariable(parentNode, OUTPUT);
				executionContext.setCurrentReportNode(previousCurrentNode);
			}
		}
		
		return output.build();
	}
}
