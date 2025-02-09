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
import step.automation.packages.AutomationPackage;
import step.automation.packages.AutomationPackageEntity;
import step.automation.packages.accessor.AutomationPackageAccessor;
import step.core.accessors.AbstractOrganizableObject;
import step.core.objectenricher.ObjectPredicate;
import step.expressions.ExpressionHandler;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class FunctionLocator {
	
	public static final String KEYWORD_ACTIVE_VERSIONS = "keyword.active.versions";
	
	private final FunctionAccessor functionAccessor;
	private final AutomationPackageAccessor apAccessor;
	private final SelectorHelper selectorHelper;
	private final ExpressionHandler expressionHandler;

	public FunctionLocator(FunctionAccessor functionAccessor, AutomationPackageAccessor apAccessor, SelectorHelper selectorHelper) {
		super();
		this.functionAccessor = functionAccessor;
		this.apAccessor = apAccessor;
		this.selectorHelper= selectorHelper;
		this.expressionHandler = new ExpressionHandler();
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
					}).findFirst().orElseThrow(()->new NoSuchElementException("Unable to find keyword with attributes "+selectionAttributesJson+" matching on of the versions: "+activeKeywordVersions));
				}
			} else {
				// Try to resolve the automation packages linked with functions and check their evaluation expressions (if exist)
				Function matchingFunction = findFirstMatchingFunctionByApEvaluationExpression(bindings, matchingFunctions);
				if (matchingFunction != null) {
					return matchingFunction;
				}

				// No active versions defined. Return the first function
				function = matchingFunctions.stream().findFirst().orElseThrow(()->new NoSuchElementException("Unable to find keyword with attributes "+selectionAttributesJson));
			}
			return function;
		} else {
			throw new NoSuchElementException("No selection attribute defined");
		}

	}

	protected Function findFirstMatchingFunctionByApEvaluationExpression(Map<String, Object> bindings, List<Function> matchingFunctions) {
		// TODO: think about performance (caching)
		for (Function matchingFunction : matchingFunctions) {
			String automationPackageId = matchingFunction.getCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_ID, String.class);
			if (automationPackageId != null && !automationPackageId.isEmpty()) {
				AutomationPackage ap = apAccessor.get(automationPackageId);
				if (ap != null) {
					String activationExpressionFromAp = ap.getCustomField(AutomationPackageEntity.AUTOMATION_PACKAGE_ACTIVATION_EXPR, String.class);
					if (activationExpressionFromAp != null && !activationExpressionFromAp.isEmpty()) {
						Object evalResult = expressionHandler.evaluateGroovyExpression(activationExpressionFromAp, bindings);
						if (evalResult != null) {
							if (!(evalResult instanceof Boolean)) {
								throw new RuntimeException("The evaluation result of activation expression for AP '"
										+ ap.getAttribute(AbstractOrganizableObject.NAME)
										+ "' is illegal. The expression should return the boolean result"
								);
							}
							if ((boolean) evalResult) {
								return matchingFunction;
							}
						}
					}
				}
			}
		}
		return null;
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
