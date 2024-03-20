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
package step.artefacts.handlers.functions;

import step.artefacts.CallFunction;
import step.artefacts.handlers.FunctionGroupHandler;
import step.artefacts.handlers.TokenSelectorHelper;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.functions.Function;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.grid.tokenpool.Interest;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class TokenSelectionCriteriaMapBuilder {

	private static final String ROUTE_TO = "route_to_";

	private final FunctionTypeRegistry functionTypeRegistry;
	protected final TokenSelectorHelper tokenSelectorHelper;

	public TokenSelectionCriteriaMapBuilder(FunctionTypeRegistry functionTypeRegistry, DynamicJsonObjectResolver dynamicJsonObjectResolver) {
		this.tokenSelectorHelper = new TokenSelectorHelper(dynamicJsonObjectResolver);
		this.functionTypeRegistry = functionTypeRegistry;
	}

	public boolean isLocalTokenRequired(CallFunction callFunction, Function function) {
		return function.requiresLocalExecution() || callFunction.getRemote().get().equals(false);
	}

	public Map<String, Interest> buildSelectionCriteriaMap(CallFunction callFunction, Function function, FunctionGroupHandler.FunctionGroupContext functionGroupContext, Map<String, Object> bindings) {
		Map<String, Interest> selectionCriteria = new HashMap<>();

		// Criteria from CallFunction Artefact
		addSelectionCriteriaFromCallFunctionArtefact(callFunction, bindings, selectionCriteria);

		// Criteria from Session Artefact if available
		addSelectionCriteriaFromFunctionGroupContextIfAvailable(functionGroupContext, selectionCriteria);

		// Criteria from function type
		// TODO As a workaround we're ignoring null functionTypeRegistry. Remove this in the future
		if (functionTypeRegistry != null) {
			AbstractFunctionType<Function> functionType = functionTypeRegistry.getFunctionTypeByFunction(function);
			Map<String, Interest> tokenSelectionCriteriaFromFunctionType = functionType.getTokenSelectionCriteria(function);
			if (tokenSelectionCriteriaFromFunctionType != null) {
				selectionCriteria.putAll(tokenSelectionCriteriaFromFunctionType);
			}
		}

		// Criteria from function
		Map<String, String> tokenSelectionCriteriaFromFunction = function.getTokenSelectionCriteria();
		if (tokenSelectionCriteriaFromFunction != null) {
			tokenSelectionCriteriaFromFunction.keySet().stream().forEach(key -> selectionCriteria.put(key, new Interest(Pattern.compile(tokenSelectionCriteriaFromFunction.get(key)), true)));
		}

		// Criteria from bindings (Special variable "route_to_")
		addTokenSelectionCriteriaFromBindings(selectionCriteria, bindings);
		return selectionCriteria;
	}

	private void addSelectionCriteriaFromFunctionGroupContextIfAvailable(FunctionGroupHandler.FunctionGroupContext functionGroupContext, Map<String, Interest> selectionCriteria) {
		if (functionGroupContext != null && functionGroupContext.getFunctionGroupTokenSelectionCriteria() != null) {
			selectionCriteria.putAll(functionGroupContext.getFunctionGroupTokenSelectionCriteria());
		}
	}

	private void addSelectionCriteriaFromCallFunctionArtefact(CallFunction callFunction, Map<String, Object> bindings, Map<String, Interest> selectionCriteria) {
		selectionCriteria.putAll(tokenSelectorHelper.getTokenSelectionCriteria(callFunction, bindings));
	}

	private Map<String, Interest> addTokenSelectionCriteriaFromBindings(Map<String, Interest> addtionalSelectionCriteria, Map<String, Object> bindings) {
		bindings.forEach((k, v) -> {
			if (v != null) {
				if (k.startsWith(ROUTE_TO)) {
					Pattern selectionPattern = Pattern.compile(v.toString());
					addtionalSelectionCriteria.put(k.replaceFirst(ROUTE_TO, ""), new Interest(selectionPattern, true));
				}
			}
		});
		return addtionalSelectionCriteria;
	}

}
