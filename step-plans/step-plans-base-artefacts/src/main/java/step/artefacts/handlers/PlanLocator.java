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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.CallPlan;
import step.core.objectenricher.ObjectPredicate;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;

public class PlanLocator {

	private static final Logger log = LoggerFactory.getLogger(PlanLocator.class);
	
	private final PlanAccessor accessor;
	private final SelectorHelper selectorHelper;

	public PlanLocator(PlanAccessor accessor, SelectorHelper selectorHelper) {
		super();
		this.accessor = accessor;
		this.selectorHelper = selectorHelper;
	}
	
	/**
	 * Resolve a {@link CallPlan} artefact to the underlying {@link Plan}. If multiple plans are resolved, the first one (ordered by priority) is returned)
	 * 
	 * @param artefact the {@link CallPlan} artefact
	 * @param objectPredicate the predicate to be used to filter the results out
	 * @param bindings the bindings to be used for the evaluation of dynamic expressions (can be null)
	 * @return the {@link Plan} referenced by the provided artefact
	 */
	public Plan selectPlan(CallPlan artefact, ObjectPredicate objectPredicate, Map<String, Object> bindings) {
		return selectAlPlansByAttributesAndPriority(artefact, objectPredicate, bindings, true).get(0);
	}

	/**
	 * Resolve a {@link CallPlan} artefact to the list of underlying matching {@link Plan}.
	 * @param artefact the {@link CallPlan} artefact
	 * @param objectPredicate the predicate to be used to filter the results out
	 * @param bindings the bindings to be used for the evaluation of dynamic expressions (can be null)
	 * @param strictMode whether selection is strict and must find a result or we can ignore unresolvable dynamic selection criteria and bypass activation expression
	 * @return the list of resolved Plan, can be empty when strictMode is false
	 */
	public List<Plan> selectAlPlansByAttributesAndPriority(CallPlan artefact, ObjectPredicate objectPredicate, Map<String, Object> bindings, boolean strictMode) {
		Objects.requireNonNull(artefact, "The artefact must not be null");
		Objects.requireNonNull(objectPredicate, "The object predicate must not be null");

		// Handle CallPlan with plan ID reference
		if(artefact.getPlanId()!=null) {
			Plan plan = accessor.get(artefact.getPlanId());
			if (plan != null && objectPredicate.test(plan)) {
				return List.of(plan);
			} else if (strictMode) {
				throw new NoSuchElementException("Unable to find plan with id: " + artefact.getPlanId());
			} else {
				return List.of();
			}
		} else {
			// Handle Call Plan with call by attributes
			String selectionAttributesJson = artefact.getSelectionAttributes().get();
			Map<String, String> selectionAttributes;
			try {
				selectionAttributes = selectorHelper.buildSelectionAttributesMap(selectionAttributesJson, bindings);
			} catch (Exception e) {
				//In case bindings are missing, we only throw an exception in strict Mode (used in execution context)
				if (strictMode) {
					throw e;
				} else {
					return List.of();
				}
			}
			Stream<Plan> stream = StreamSupport.stream(accessor.findManyByAttributes(selectionAttributes), false);
			stream = stream.filter(objectPredicate);
			List<Plan> matchingPlans = stream.collect(Collectors.toList());

			// The same logic as for functions - plans from current automation package have priority in 'CallPlan'
			// We use prioritization by current automation package and filtering by activation expressions
			List<Plan> orderedPlans = LocatorHelper.prioritizeAndFilterApEntities(matchingPlans, bindings, !strictMode);
			if (strictMode && orderedPlans.isEmpty()) {
				throw new NoSuchElementException("Unable to find plan with attributes: "+ selectionAttributesJson);
			}
			return orderedPlans;
		}
	}

	/**
	 * Resolve a {@link CallPlan} artefact to the list of underlying matching {@link Plan}.
	 *
	 * @param artefact the {@link CallPlan} artefact
	 * @param objectPredicate the predicate to be used to filter the results out
	 * @param bindings the bindings to be used for the evaluation of dynamic expressions (can be null)
	 * @return the list of {@link Plan} referenced by this artifact
	 */
	public List<Plan> getMatchingPlans(CallPlan artefact, ObjectPredicate objectPredicate, Map<String, Object> bindings) {
		return selectAlPlansByAttributesAndPriority(artefact, objectPredicate, bindings, false);
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
