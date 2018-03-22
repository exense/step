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
import java.util.Map;

import javax.json.JsonObject;
import javax.json.spi.JsonProvider;

import step.artefacts.CallPlan;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactAccessor;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.execution.ExecutionContext;

public class CallPlanHandler extends ArtefactHandler<CallPlan, ReportNode> {

	private static JsonProvider jprov = JsonProvider.provider();

	protected DynamicJsonObjectResolver dynamicJsonObjectResolver;
	
	@Override
	public void init(ExecutionContext context) {
		super.init(context);
		dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getGlobalContext().getExpressionHandler()));
	}

	
	@Override
	protected void createReportSkeleton_(ReportNode parentNode,	CallPlan testArtefact) {
		beforeDelegation(parentNode, testArtefact);
		AbstractArtefact a = selectArtefact(testArtefact);
		delegateCreateReportSkeleton(a, parentNode);
	}

	private void beforeDelegation(ReportNode parentNode, CallPlan testArtefact) {
		context.getVariablesManager().putVariable(parentNode, "#placeholder", testArtefact);

		String inputJson = (testArtefact.getInput().get()!=null)?testArtefact.getInput().get():"{}";
		JsonObject input = jprov.createReader(new StringReader(inputJson)).readObject();
		JsonObject resolvedInput = dynamicJsonObjectResolver.evaluate(input, getBindings());		
		context.getVariablesManager().putVariable(parentNode, "input", resolvedInput);
	}
	
	
	

	@Override
	protected void execute_(ReportNode node, CallPlan testArtefact) {
		beforeDelegation(node, testArtefact);

		AbstractArtefact a = selectArtefact(testArtefact);
		
		ReportNode resultNode = delegateExecute(context, a, node);
		node.setStatus(resultNode.getStatus());
	}

	protected AbstractArtefact selectArtefact(CallPlan testArtefact) {
		AbstractArtefact a;
		ArtefactAccessor artefactAccessor = context.getGlobalContext().getArtefactAccessor();
		if(testArtefact.getArtefactId()!=null) {
			a =  context.getGlobalContext().getArtefactAccessor().get(testArtefact.getArtefactId());
		} else {
			DynamicJsonObjectResolver dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getGlobalContext().getExpressionHandler()));
			SelectorHelper selectorHelper = new SelectorHelper(dynamicJsonObjectResolver);
			Map<String, String> selectionAttributes = selectorHelper.buildSelectionAttributesMap(testArtefact.getSelectionAttributes().get(), getBindings());
			a = artefactAccessor.findRootArtefactByAttributes(selectionAttributes);
			if(a==null) {
				throw new RuntimeException("Unable to find plan with attributes: "+selectionAttributes.toString());
			}
		}
		return a;
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, CallPlan testArtefact) {
		return new ReportNode();
	}

}
