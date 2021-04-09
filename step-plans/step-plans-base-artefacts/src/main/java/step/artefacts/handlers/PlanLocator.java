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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import step.artefacts.CallPlan;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionContextBindings;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.functions.Function;

public class PlanLocator {
	
	protected ExecutionContext context;
	protected PlanAccessor accessor;
	protected SelectorHelper selectorHelper;

	public Plan selectPlan(CallPlan artefact) {
		return selectPlan(artefact, null);
	}

	public Plan selectPlan(CallPlan artefact, ObjectPredicate sessionObjectPredicate) {
		Plan a;
		
		if(artefact.getPlanId()!=null) {
			a =  accessor.get(artefact.getPlanId());
		} else {
			ObjectPredicate objectPredicate = (context!=null) ? context.getObjectPredicate() : sessionObjectPredicate;
			Map<String, String> selectionAttributes = selectorHelper.buildSelectionAttributesMap(artefact.getSelectionAttributes().get(), getBindings());
			//a = accessor.findByAttributes(selectionAttributes);
			Stream<Plan> stream = StreamSupport.stream(accessor.findManyByAttributes(selectionAttributes), false);
			if(objectPredicate != null) {
				stream = stream.filter(objectPredicate);
			}
			List<Plan> matchingFunctions = stream.collect(Collectors.toList());
			a = matchingFunctions.stream().findFirst().orElseThrow(()->new RuntimeException("Unable to find plan with attributes: "+selectionAttributes.toString()));
		}
		return a;
	}
	
	private Map<String, Object> getBindings() {
		if (context != null) {
			return ExecutionContextBindings.get(context);
		} else {
			return null;
		}
	}

	public PlanLocator(ExecutionContext context, PlanAccessor accessor, SelectorHelper selectorHelper) {
		super();
		this.context = context;
		this.accessor = accessor;
		this.selectorHelper = selectorHelper;
	}

}
