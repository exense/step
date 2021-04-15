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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import step.artefacts.CallPlan;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;

public class PlanLocator {
	
	private final PlanAccessor accessor;
	private final SelectorHelper selectorHelper;

	public PlanLocator(PlanAccessor accessor, SelectorHelper selectorHelper) {
		super();
		this.accessor = accessor;
		this.selectorHelper = selectorHelper;
	}
	
	/**
	 * Resolve a {@link CallPlan} artefact to the underlying {@link Plan}
	 * 
	 * @param callFunctionArtefact the {@link CallPlan} artefact
	 * @param objectPredicate the predicate to be used to filter the results out
	 * @param bindings the bindings to be used for the evaluation of dynamic expressions (can be null)
	 * @return the {@link Plan} referenced by the provided artefact
	 */
	public Plan selectPlan(CallPlan artefact, ObjectPredicate objectPredicate, Map<String, Object> bindings) {
		Objects.requireNonNull(artefact, "The artefact must not be null");
		Objects.requireNonNull(objectPredicate, "The object predicate must not be null");

		Plan a;
		if(artefact.getPlanId()!=null) {
			a =  accessor.get(artefact.getPlanId());
		} else {
			Map<String, String> selectionAttributes = selectorHelper.buildSelectionAttributesMap(artefact.getSelectionAttributes().get(), bindings);
			Stream<Plan> stream = StreamSupport.stream(accessor.findManyByAttributes(selectionAttributes), false);
			stream = stream.filter(objectPredicate);
			List<Plan> matchingFunctions = stream.collect(Collectors.toList());
			a = matchingFunctions.stream().findFirst().orElseThrow(()->new RuntimeException("Unable to find plan with attributes: "+selectionAttributes.toString()));
		}
		return a;
	}
}
