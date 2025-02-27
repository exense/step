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

import step.artefacts.CallFunction;
import step.automation.packages.AutomationPackageEntity;
import step.commons.activation.Activator;
import step.commons.activation.Expression;
import step.core.accessors.AbstractOrganizableObject;
import step.core.execution.ExecutionContextBindings;
import step.core.objectenricher.ObjectPredicate;
import step.functions.EvaluationExpression;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;

import javax.script.SimpleBindings;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class FunctionLocator {
	
	public static final String KEYWORD_ACTIVE_VERSIONS = "keyword.active.versions";
	
	private final FunctionAccessor functionAccessor;
	private final SelectorHelper selectorHelper;

	public FunctionLocator(FunctionAccessor functionAccessor, SelectorHelper selectorHelper) {
		super();
		this.functionAccessor = functionAccessor;
		this.selectorHelper= selectorHelper;
	}

	/**
	 * Resolve a {@link CallFunction} artefact to the underlying {@link Function}
	 * 
	 * @param callFunctionArtefact the CallFunction artefact
	 * @param objectPredicate the predicate to be used to filter the results out
	 * @param bindings the bindings to be used for the evaluation of dynamic expressions (can be null)
	 * @return the {@link Function} referenced by this artefact
	 */
	public Function getFunction(CallFunction callFunctionArtefact, ObjectPredicate objectPredicate, Map<String, Object> bindings) {
		Objects.requireNonNull(callFunctionArtefact, "The artefact must not be null");
		Objects.requireNonNull(objectPredicate, "The object predicate must not be null");
		Function function;
		String selectionAttributesJson = callFunctionArtefact.getFunction().get();
		Map<String, String> attributes;
		try {
			attributes = selectorHelper.buildSelectionAttributesMap(selectionAttributesJson, bindings);
		} catch (Exception e) {
			throw new NoSuchElementException("Unable to find keyword with attributes "+selectionAttributesJson + ". Cause: " + e.getMessage());
		}

		if(attributes.size()>0) {

			Stream<Function> stream = StreamSupport.stream(functionAccessor.findManyByAttributes(attributes), false);
			stream = stream.filter(objectPredicate);
			List<Function> functionsMatchingByAttributes = stream.collect(Collectors.toList());

			// reorder matching functions: the function from current AP has a priority
			List<Function> orderedFunctions = prioritizeAndFilterApEntities(functionsMatchingByAttributes, bindings);

			// after prioritization, we check the chosen active keyword version
			Set<String> activeKeywordVersions = getActiveKeywordVersions(bindings);
			if (activeKeywordVersions != null && activeKeywordVersions.size() > 0) {
				// First try to find a function matching one of the active versions
				function = orderedFunctions.stream().filter(f -> {
					String version = f.getAttributes().get(AbstractOrganizableObject.VERSION);
					return version != null && activeKeywordVersions.contains(version);
				}).findFirst().orElse(null);
				// if no function has been found with one of the active versions, return the first function WITHOUT version
				if (function == null) {
					function = orderedFunctions.stream().filter(f -> {
						String version = f.getAttributes().get(AbstractOrganizableObject.VERSION);
						return version == null || version.trim().isEmpty();
					}).findFirst().orElseThrow(() -> new NoSuchElementException("Unable to find keyword with attributes " + selectionAttributesJson + " matching on of the versions: " + activeKeywordVersions));
				}
			} else {
				// No active versions defined. Return the first function
				function = orderedFunctions.stream().findFirst().orElseThrow(() -> new NoSuchElementException("Unable to find keyword with attributes " + selectionAttributesJson));
			}
			return function;
		} else {
			throw new NoSuchElementException("No selection attribute defined");
		}

	}

	/**
	 * Reorders and filters entities according to the current automation package and activation expression
	 */
	public static <T extends AbstractOrganizableObject> List<T> prioritizeAndFilterApEntities(List<T> entities, Map<String, Object> bindings) {
		// reorder entities: entities from current AP have a priority
		List<T> entitiesFromSameAP = new ArrayList<>();
		List<T> entitiesActivatedExplicitly = new ArrayList<>();
		List<T> entitiesWithLowerPriority = new ArrayList<>();
		String planApId = (bindings != null) ? (String) bindings.get(ExecutionContextBindings.BINDING_AP) : null;
		for (T entity : entities) {
			// If we're executing a plan belonging to an AP, any matching entity from the same AP as the priority
			// activation expression is not checked since all entity from the AP have the same activation expression, it should
			// not impact the selection
			if (planApId != null) {
				String entityApId = entity.getCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_ID, String.class);
				if (Objects.equals(entityApId, planApId)) {
					entitiesFromSameAP.add(entity);
					break;
				}
			}
			if (evaluationExprressionIsDefined(entity)) {
				if (isActivated(bindings, entity)) {
					entitiesActivatedExplicitly.add(entity);
				}
			} else {
				entitiesWithLowerPriority.add(entity);
			}
		}

		List<T> orderedEntities = new ArrayList<>();
		orderedEntities.addAll(entitiesFromSameAP);
		orderedEntities.addAll(entitiesActivatedExplicitly);
		orderedEntities.addAll(entitiesWithLowerPriority);
		return orderedEntities;
	}

	private static <T extends AbstractOrganizableObject> boolean evaluationExprressionIsDefined(T entity) {
		return (entity instanceof EvaluationExpression && ((EvaluationExpression) entity).getActivationExpression() != null);
	}

	private static <T extends AbstractOrganizableObject> boolean isActivated(Map<String, Object> bindings, T entity) {
		if (evaluationExprressionIsDefined(entity)) {
			Expression activationExpression = ((EvaluationExpression) entity).getActivationExpression();
			return Activator.evaluateActivationExpression(bindings == null ? null : new SimpleBindings(bindings), activationExpression, Activator.DEFAULT_SCRIPT_ENGINE);
		} else {
			return true;
		}
	}

	private Set<String> getActiveKeywordVersions(Map<String, Object> bindings) {
		Set<String> activeKeywordVersions = null;
		if (bindings != null) {
			
			Object activeKeywordVersionsObject = bindings.get(KEYWORD_ACTIVE_VERSIONS);
			activeKeywordVersions = new HashSet<>();
			if(activeKeywordVersionsObject != null) {
				activeKeywordVersions.addAll(Arrays.asList(activeKeywordVersionsObject.toString().split(",")));
			}
		}
		return activeKeywordVersions;
	}
}
