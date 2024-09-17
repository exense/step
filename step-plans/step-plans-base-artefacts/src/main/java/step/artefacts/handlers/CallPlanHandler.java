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
package step.artefacts.handlers;

import jakarta.json.JsonObject;
import step.artefacts.CallPlan;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.core.dynamicbeans.DynamicJsonValueResolver;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionContextBindings;
import step.core.json.JsonProviderCache;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.functions.accessor.FunctionAccessor;

import java.io.StringReader;
import java.util.Map;

public class CallPlanHandler extends ArtefactHandler<CallPlan, ReportNode> {

	protected DynamicJsonObjectResolver dynamicJsonObjectResolver;
	
	protected PlanLocator planLocator;
	
	@Override
	public void init(ExecutionContext context) {
		super.init(context);
		dynamicJsonObjectResolver = new DynamicJsonObjectResolver(new DynamicJsonValueResolver(context.getExpressionHandler()));
		planLocator = new PlanLocator(context.getPlanAccessor(), new SelectorHelper(dynamicJsonObjectResolver));
	}
	
	@Override
	protected void createReportSkeleton_(ReportNode parentNode,	CallPlan testArtefact) {
		beforeDelegation(parentNode, testArtefact);
		Plan a = selectPlan(testArtefact);

		delegateCreateReportSkeleton(a.getRoot(), parentNode);
	}

	private void beforeDelegation(ReportNode parentNode, CallPlan testArtefact) {
		context.getVariablesManager().putVariable(parentNode, "#placeholder", testArtefact);

		String inputJson = (testArtefact.getInput().get()!=null)?testArtefact.getInput().get():"{}";
		JsonObject input = JsonProviderCache.createReader(new StringReader(inputJson)).readObject();
		JsonObject resolvedInput = dynamicJsonObjectResolver.evaluate(input, getBindings());		
		context.getVariablesManager().putVariable(parentNode, "input", resolvedInput);
	}

	@Override
	protected void execute_(ReportNode node, CallPlan testArtefact) {
		beforeDelegation(node, testArtefact);

		// Append the artefactId of the current artefact to the path
		pushArtefactPath(node, testArtefact);

		Plan a = selectPlan(testArtefact);
		
		ReportNode resultNode = delegateExecute(a.getRoot(), node);
		node.setStatus(resultNode.getStatus());
	}

	@Override
	public AbstractArtefact resolveArtefactCall(CallPlan artefact) {
		Plan plan = selectPlan(artefact);
		return plan != null ? plan.getRoot() : null;
	}

	protected Plan selectPlan(CallPlan testArtefact) {
		return planLocator.selectPlan(testArtefact, context.getObjectPredicate(),
				ExecutionContextBindings.get(context));
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, CallPlan testArtefact) {
		return new ReportNode();
	}

}
