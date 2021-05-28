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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.bson.types.ObjectId;

import step.artefacts.CallFunction;
import step.core.accessors.AbstractOrganizableObject;
import step.core.objectenricher.ObjectPredicate;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;

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
		Function function = null;
		if(callFunctionArtefact.getFunctionId()!=null) {
			function = functionAccessor.get(new ObjectId(callFunctionArtefact.getFunctionId()));
			if(function == null) {
				throw new RuntimeException("Unable to find keyword with id "+callFunctionArtefact.getFunctionId());
			}
		} else {
			String selectionAttributesJson = callFunctionArtefact.getFunction().get();
			Map<String, String> attributes = selectorHelper.buildSelectionAttributesMap(selectionAttributesJson, bindings);
			
			Stream<Function> stream = StreamSupport.stream(functionAccessor.findManyByAttributes(attributes), false);
			stream = stream.filter(objectPredicate);
			List<Function> matchingFunctions = stream.collect(Collectors.toList());
			
			Set<String> activeKeywordVersions = getActiveKeywordVersions(bindings);
			if(activeKeywordVersions != null && activeKeywordVersions.size()>0) {
				// First try to find a function matching one of the active versions
				function = matchingFunctions.stream().filter(f->{
					String version = f.getAttributes().get(AbstractOrganizableObject.VERSION);
					return version != null && activeKeywordVersions.contains(version);
				}).findFirst().orElse(null);
				// if no function has been found with one of the active versions, return the first function WITHOUT version
				if(function == null) {
					function = matchingFunctions.stream().filter(f->{
						String version = f.getAttributes().get(AbstractOrganizableObject.VERSION);
						return version == null || version.trim().isEmpty();
					}).findFirst().orElseThrow(()->new RuntimeException("Unable to find keyword with attributes "+selectionAttributesJson+" matching on of the versions: "+activeKeywordVersions));
				}
			} else {
				// No active versions defined. Return the first function
				function = matchingFunctions.stream().findFirst().orElseThrow(()->new RuntimeException("Unable to find keyword with attributes "+selectionAttributesJson));
			}
		}
				
		return function;
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
