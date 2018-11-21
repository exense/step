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
package step.artefacts.handlers;

import java.io.StringReader;

import javax.json.JsonObject;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonParsingException;

import step.artefacts.Return;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.execution.ExecutionContext;
import step.functions.io.OutputBuilder;

public class ReturnHandler extends ArtefactHandler<Return, ReportNode> {
	
	private static JsonProvider jprov = JsonProvider.provider();
	
	protected DynamicJsonObjectResolver dynamicJsonObjectResolver;

	@Override
	public void init(ExecutionContext context) {
		dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler()));
		super.init(context);
	}

	@Override
	protected void createReportSkeleton_(ReportNode parentNode, Return testArtefact) {

	}

	@Override
	protected void execute_(ReportNode node, Return testArtefact) {
		node.setStatus(ReportNodeStatus.PASSED);

		Object o = context.getVariablesManager().getVariable("output");
		if(o!=null && o instanceof OutputBuilder) {
			JsonObject outputJsonAfterResolving = resolveOutputJson(testArtefact);
			((OutputBuilder)o).setPayloadJson(outputJsonAfterResolving.toString());
		}
	}

	private JsonObject resolveOutputJson(Return testArtefact) {
		String outputJson = testArtefact.getOutput().get();
		
		JsonObject outputJsonObject;
		try {
			if(outputJson!=null&&outputJson.trim().length()>0) {
				outputJsonObject = jprov.createReader(new StringReader(outputJson)).readObject();
			} else {
				outputJsonObject = jprov.createObjectBuilder().build();
			}
		} catch(JsonParsingException e) {
			throw new RuntimeException("Error while parsing argument (input): "+e.getMessage());
		}
		
		JsonObject outputJsonAfterResolving = dynamicJsonObjectResolver.evaluate(outputJsonObject, getBindings());
		return outputJsonAfterResolving;
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, Return testArtefact) {
		return new ReportNode();
	}
}
