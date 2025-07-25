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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.CallPlan;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;
import step.entities.activation.Activator;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class PlanLocator {

	private static final Logger log = LoggerFactory.getLogger(PlanLocator.class);
	
	private final PlanAccessor accessor;
	private final SelectorHelper selectorHelper;
	private final ApEntitiesPrioritizer apEntitiesPrioritizer;

	public PlanLocator(PlanAccessor accessor, SelectorHelper selectorHelper, Activator activator) {
		super();
		this.accessor = accessor;
		this.selectorHelper = selectorHelper;
		this.apEntitiesPrioritizer = new ApEntitiesPrioritizer(activator);
	}
	
	/**
	 * Resolve a {@link CallPlan} artefact to the underlying {@link Plan}. Returns null if plan is not resolved by ID
	 * 
	 * @param artefact the {@link CallPlan} artefact
	 * @param objectPredicate the predicate to be used to filter the results out
	 * @param bindings the bindings to be used for the evaluation of dynamic expressions (can be null)
	 * @return the {@link Plan} referenced by the provided artefact
	 */
	public Plan selectPlan(CallPlan artefact, ObjectPredicate objectPredicate, Map<String, Object> bindings) {
		Objects.requireNonNull(artefact, "The artefact must not be null");
		Objects.requireNonNull(objectPredicate, "The object predicate must not be null");

		Plan a;
		if(artefact.getPlanId()!=null) {
			a = Optional.ofNullable(accessor.get(artefact.getPlanId())).orElseThrow(() -> new  NoSuchElementException("Unable to find plan with id: " + artefact.getPlanId()));
		} else {
			Map<String, String> selectionAttributes = selectorHelper.buildSelectionAttributesMap(artefact.getSelectionAttributes().get(), bindings);
			Stream<Plan> stream = StreamSupport.stream(accessor.findManyByAttributes(selectionAttributes), false);
			stream = stream.filter(objectPredicate);
			List<Plan> matchingPlans = stream.collect(Collectors.toList());

			// The same logic as for functions - plans from current automation package have priority in 'CallPlan'
			// We use prioritization by current automation package and filtering by activation expressions
			List<Plan> orderedPlans = apEntitiesPrioritizer.prioritizeAndFilterApEntities(matchingPlans, bindings);
			a = orderedPlans.stream().findFirst().orElseThrow(()->new NoSuchElementException("Unable to find plan with attributes: "+selectionAttributes.toString()));
		}
		return a;
	}

	/**
	 * Resolve a {@link CallPlan} artefact to the underlying {@link Plan}. Throws an exception if plan is not resolved for any reason
	 *
	 * @param artefact        the {@link CallPlan} artefact
	 * @param objectPredicate the predicate to be used to filter the results out
	 * @param bindings        the bindings to be used for the evaluation of dynamic expressions (can be null)
	 * @return the {@link Plan} referenced by the provided artefact
	 */
	public Plan selectPlanNotNull(CallPlan artefact, ObjectPredicate objectPredicate, Map<String, Object> bindings) throws PlanLocatorException {
		Plan p;
		try {
			p = selectPlan(artefact, objectPredicate, bindings);
		} catch (Exception ex) {
			log.warn("Unable to resolve call plan", ex);
			throw new PlanLocatorException(createPlanNotResolvedMessage(artefact), ex);
		}
		if (p == null) {
			throw new PlanLocatorException(createPlanNotResolvedMessage(artefact));
		}
		return p;
	}

	private String createPlanNotResolvedMessage(CallPlan artefact) {
		String planId = artefact.getPlanId();
		String selectionAttributes = artefact.getSelectionAttributes().get();
		return "Could not resolve called plan with ID:'" + (planId == null ? "" : planId) + "'; Selection attributes: '" + (selectionAttributes == null ? "" : selectionAttributes) + "'";
	}

	public static class PlanLocatorException extends Exception {
		public PlanLocatorException(String message) {
			super(message);
		}

		public PlanLocatorException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
