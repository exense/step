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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.bson.types.ObjectId;

import step.artefacts.CallFunction;
import step.core.accessors.AbstractOrganizableObject;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionContextBindings;
import step.core.objectenricher.ObjectPredicate;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;

public class FunctionLocator {
	
	public static final String KEYWORD_ACTIVE_VERSIONS = "keyword.active.versions";
	
	protected ExecutionContext context;
	protected FunctionAccessor functionAccessor;
	private SelectorHelper selectorHelper;
	
	public FunctionLocator(FunctionAccessor functionAccessor, SelectorHelper selectorHelper) {
		this(functionAccessor,selectorHelper, null);
	}

	public FunctionLocator(FunctionAccessor functionAccessor, SelectorHelper selectorHelper, ExecutionContext context) {
		super();
		this.functionAccessor = functionAccessor;
		this.selectorHelper= selectorHelper;
		this.context = context;
	}

	public Function getFunction(CallFunction testArtefact) {
		return getFunction(testArtefact, null);
	}

	/**
	 * Resolve a CallFunction artefact to the underlying function
	 * @param testArtefact the CallFunction artefact
	 * @param sessionObjectPredicate object predicate set in HTTP session (null if invoked from an execution)
	 * @return the Function referenced by this artefact
	 */
	public Function getFunction(CallFunction testArtefact, ObjectPredicate sessionObjectPredicate) {
		Function function = null;
		if(testArtefact.getFunctionId()!=null) {
			function = functionAccessor.get(new ObjectId(testArtefact.getFunctionId()));
			if(function == null) {
				throw new RuntimeException("Unable to find keyword with id "+testArtefact.getFunctionId());
			}
		} else {
			String selectionAttributesJson = testArtefact.getFunction().get();
			Map<String, String> attributes = selectorHelper.buildSelectionAttributesMap(selectionAttributesJson, getBindings());
			
			ObjectPredicate objectPredicate = (context!=null) ? context.getObjectPredicate() : sessionObjectPredicate;
			Stream<Function> stream = StreamSupport.stream(functionAccessor.findManyByAttributes(attributes), false);
			if(objectPredicate != null) {
				stream = stream.filter(objectPredicate);
			}
			List<Function> matchingFunctions = stream.collect(Collectors.toList());
			
			Set<String> activeKeywordVersions = getActiveKeywordVersions();
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
	
	private Map<String, Object> getBindings() {
		if (context != null) {
			return ExecutionContextBindings.get(context);
		} else {
			return null;
		}
	}

	private Set<String> getActiveKeywordVersions() {
		Set<String> activeKeywordVersions = null;
		if (context != null) {
			String activeKeywordVersionsStr = context.getVariablesManager().getVariableAsString(KEYWORD_ACTIVE_VERSIONS,null);
			activeKeywordVersions = new HashSet<>();
			if(activeKeywordVersionsStr != null) {
				activeKeywordVersions.addAll(Arrays.asList(activeKeywordVersionsStr.split(",")));
			}
		}
		return activeKeywordVersions;
	}
}
