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

import java.io.StringReader;
import java.util.Map;

import javax.json.Json;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.GlobalContext;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.deployment.JacksonMapperProvider;
import step.core.execution.ExecutionContext;
import step.core.variables.VariableType;
import step.grid.agent.handler.PropertyAwareMessageHandler;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class ArtefactMessageHandler implements PropertyAwareMessageHandler {

	
	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public OutputMessage handle(AgentTokenWrapper token, Map<String, String> properties, InputMessage message)
			throws Exception {
		ExecutionContext executionContext = ExecutionContext.getCurrentContext();
		GlobalContext globalContext = executionContext.getGlobalContext();
		
		String artefactId = properties.get("artefactid");
		String parentReportId = null; // TODO message.getProperties().get("parentreportid");;
		
		ReportNode parentNode;
		if(parentReportId == null) {
			parentNode = new ReportNode();
			globalContext.getReportAccessor().save(parentNode);
			parentReportId = parentNode.getId().toString();
		} else {
			parentNode = globalContext.getReportAccessor().get(new ObjectId(parentReportId));
		}
		
		executionContext.setCurrentReportNode(parentNode);
		executionContext.getReportNodeCache().put(parentNode);
		
		AbstractArtefact artefact = globalContext.getArtefactAccessor().get(artefactId);
		
		executionContext.getVariablesManager().putVariable(parentNode, "args", message.getArgument());
		OutputMessage output = new OutputMessage();
		executionContext.getVariablesManager().putVariable(parentNode, VariableType.RESERVED, "output", output);
		
		try {
			ReportNode node = ArtefactHandler.delegateExecute(artefact,parentNode);
			output.setError(node.getError());			
		} finally {
			executionContext.getVariablesManager().removeVariable(parentNode, "output");
		}
		
		ObjectMapper m = JacksonMapperProvider.createMapper();
		//output.setPayload(Json.createReader(new StringReader(m.writeValueAsString(node))).readObject());
		return output;
	}

}
