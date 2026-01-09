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
import step.core.accessors.AbstractOrganizableObject;
import step.core.objectenricher.ObjectPredicate;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;

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
	 * @param callFunctionArtefact the CallFunction artifact to be resolved
	 * @param objectPredicate the predicate to be used to filter the results out
	 * @param bindings the bindings to be used for the evaluation of dynamic expressions (can be null)
	 * @return the {@link Function} referenced by this artifact
	 */
	public Function getFunction(CallFunction callFunctionArtefact, ObjectPredicate objectPredicate, Map<String, Object> bindings) {
		return selectAllFunctionsByPriority(callFunctionArtefact, objectPredicate, bindings, true).get(0);
	}

	/**
	 * Resolve a {@link CallFunction} artefact to the underlying list of matching {@link Function}
	 * @param callFunctionArtefact the {@link CallFunction} artifact to resolve
	 * @param objectPredicate to filter out results
	 * @param bindings to be used for evaluation of selection criteria
	 * @param strictMode whether selection is strict and must find a result or we can ignore unresolvable dynamic selection criteria and bypass activation expression
	 * @return the list of resolved Keywords, can be empty when strictMode is false
	 */
	public List<Function> selectAllFunctionsByPriority(CallFunction callFunctionArtefact, ObjectPredicate objectPredicate, Map<String, Object> bindings, boolean strictMode) {
		Objects.requireNonNull(callFunctionArtefact, "The artefact must not be null");
		Objects.requireNonNull(objectPredicate, "The object predicate must not be null");

		String selectionAttributesJson = callFunctionArtefact.getFunction().get();
		Map<String, String> attributes;
		try {
			attributes = selectorHelper.buildSelectionAttributesMap(selectionAttributesJson, bindings);
		} catch (Exception e) {
			//We only throw exception for missing bindings when strictMode is ON
			if (strictMode) {
				throw new NoSuchElementException("Unable to find keyword with attributes " + selectionAttributesJson + ". Cause: " + e.getMessage());
			} else {
				return List.of();
			}
		}

		if (!attributes.isEmpty()) {
			Stream<Function> stream = StreamSupport.stream(functionAccessor.findManyByAttributes(attributes), false);
			stream = stream.filter(objectPredicate);
			List<Function> functionsMatchingByAttributes = stream.collect(Collectors.toList());

			// reorder matching functions: the function from current AP has a priority
			List<Function> orderedFunctions = LocatorHelper.prioritizeAndFilterApEntities(functionsMatchingByAttributes, bindings, !strictMode);
			// In strict mode at least one match is required
			if (strictMode && orderedFunctions.isEmpty()) {
				throw new NoSuchElementException("Unable to find keyword with attributes " + selectionAttributesJson);
			}

			// after prioritization, we either select only the one matching the active version whenever provided or returned all matching ones
			Set<String> activeKeywordVersions = getActiveKeywordVersions(bindings);
			if (activeKeywordVersions != null && !activeKeywordVersions.isEmpty()) {
				// First try to find the functions matching one of the active versions
				List<Function> activeVersions = orderedFunctions.stream().filter(f -> {
					String version = f.getAttributes().get(AbstractOrganizableObject.VERSION);
					return version != null && activeKeywordVersions.contains(version);
				}).collect(Collectors.toList());
				// if no function has been found with one of the active versions, return the functions WITHOUT versions
				if (activeVersions.isEmpty()) {
					activeVersions = orderedFunctions.stream().filter(f -> {
						String version = f.getAttributes().get(AbstractOrganizableObject.VERSION);
						return version == null || version.trim().isEmpty();
					}).collect(Collectors.toList());
				}
				if (activeVersions.isEmpty() && strictMode) {
					throw new NoSuchElementException("Unable to find keyword with attributes " + selectionAttributesJson + " matching on of the versions: " + activeKeywordVersions);
				} else {
					return activeVersions;
				}
			} else {
				//No active version provided, we simply return the ordered function by priorities
				return orderedFunctions;
			}
		} else {
			throw new NoSuchElementException("No selection attribute defined");
		}
	}

	/**
	 * Resolve a {@link CallFunction} artefact to the underlying list of matching {@link Function}
	 *
	 * @param callFunctionArtefact the CallFunction artifact
	 * @param objectPredicate the predicate to be used to filter the results out
	 * @param bindings the bindings to be used for the evaluation of dynamic expressions (can be null)
	 * @return the {@link Function} list referenced by this artifact
	 */
	public List<Function> getMatchingFunctions(CallFunction callFunctionArtefact, ObjectPredicate objectPredicate, Map<String, Object> bindings) {
		return selectAllFunctionsByPriority(callFunctionArtefact, objectPredicate, bindings, false);
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
