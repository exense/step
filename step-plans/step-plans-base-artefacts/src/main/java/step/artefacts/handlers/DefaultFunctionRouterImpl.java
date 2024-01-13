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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import step.artefacts.CallFunction;
import step.artefacts.handlers.FunctionGroupHandler.FunctionGroupContext;
import step.common.managedoperations.OperationManager;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.functions.Function;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.grid.TokenWrapper;
import step.grid.TokenWrapperOwner;
import step.grid.tokenpool.Identity;
import step.grid.tokenpool.Interest;
import step.grid.tokenpool.SimpleAffinityEvaluator;

public class DefaultFunctionRouterImpl implements FunctionRouter {

	protected final TokenSelectorHelper tokenSelectorHelper;

	protected final FunctionExecutionService functionExecutionService;

	protected final FunctionTypeRegistry functionTypeRegistry;
	
	protected final SimpleAffinityEvaluator<Identity,Identity> affinityEvaluator = new SimpleAffinityEvaluator<>();

	protected final LocalFunctionRouterImpl localFunctionRouter;

	public DefaultFunctionRouterImpl(FunctionExecutionService functionClient, FunctionTypeRegistry functionTypeRegistry, DynamicJsonObjectResolver dynamicJsonObjectResolver) {
		super();
		this.functionExecutionService = functionClient;
		this.functionTypeRegistry = functionTypeRegistry;
		this.tokenSelectorHelper = new TokenSelectorHelper(dynamicJsonObjectResolver);
		this.localFunctionRouter = new LocalFunctionRouterImpl(functionClient);
	}

	@Override
	public TokenWrapper selectToken(CallFunction callFunction, Function function, FunctionGroupContext functionGroupContext, Map<String, Object> bindings, TokenWrapperOwner tokenWrapperOwner) throws FunctionExecutionServiceException {
		TokenWrapper token;
		if(function.requiresLocalExecution() || callFunction.getRemote().get().equals(false)) {
			// The function requires a local execution => get a local token
			token = localFunctionRouter.selectToken(callFunction, function, functionGroupContext, bindings, tokenWrapperOwner);
		} else {
			if(functionGroupContext!=null) {
				synchronized (functionGroupContext) {
					if(!functionGroupContext.isOwner(Thread.currentThread().getId())) {
						throw new RuntimeException("Tokens from this sesssion are already reserved by another thread. This usually means that you're spawning threads from wihtin a session control without creating new sessions for the new threads.");
					}
					Map<String, Interest> selectionCriteria = buildSelectionCriteriaMap(callFunction, function,	functionGroupContext, bindings);
					
					// Find a token matching the selection criteria in the context
					List<TokenWrapper> functionGroupTokens = functionGroupContext.getTokens();
					TokenWrapper matchingToken = functionGroupTokens.stream().filter(t->
						affinityEvaluator.getAffinityScore(identity(selectionCriteria, null), identity(null, t.getAttributes())) > 0).findFirst().orElse(null);
					
					if(matchingToken != null) {
						// Token already present in context => reusing it
						token = matchingToken;
					} else {
						// No token matching the selection criteria => select a new token and add it to the function group context
						token = selectToken(selectionCriteria, true, tokenWrapperOwner);
						functionGroupContext.addToken(token);
					}
				}
			} else {
				// No FunctionGroupContext. Simply select a token without creating an agent session
				Map<String, Interest> selectionCriteria = buildSelectionCriteriaMap(callFunction, function,	functionGroupContext, bindings);
				token = selectToken(selectionCriteria, false, tokenWrapperOwner);
			}
		}
		return token;
	}

	protected Identity identity(Map<String, Interest> selectionCriteria, Map<String, String> attributes) {
		return new Identity() {
			
			@Override
			public Map<String, Interest> getInterests() {
				return selectionCriteria;
			}
			
			@Override
			public String getID() {
				return null;
			}
			
			@Override
			public Map<String, String> getAttributes() {
				return attributes;
			}
		};
	}

	private TokenWrapper selectToken(Map<String, Interest> selectionCriteria, boolean createSession, TokenWrapperOwner tokenWrapperOwner) throws FunctionExecutionServiceException {		
		Map<String, String> pretenderAttributes = new HashMap<>();

		TokenWrapper token;
		OperationManager.getInstance().enter("Token selection", selectionCriteria);
		try {
			token = functionExecutionService.getTokenHandle(pretenderAttributes, selectionCriteria, createSession, tokenWrapperOwner);
		} finally {
			OperationManager.getInstance().exit();					
		}
		return token;
	}

	protected Map<String, Interest> buildSelectionCriteriaMap(CallFunction callFunction, Function function, FunctionGroupContext functionGroupContext, Map<String, Object> bindings) {
		Map<String, Interest> selectionCriteria = new HashMap<>();

		// Criteria from CallFunction Artefact
		selectionCriteria.putAll(tokenSelectorHelper.getTokenSelectionCriteria(callFunction, bindings));

		// Criteria from Session Artefact if available
		if(functionGroupContext!=null && functionGroupContext.getAdditionalSelectionCriteria()!=null) {
			selectionCriteria.putAll(functionGroupContext.getAdditionalSelectionCriteria());
		}

		// Criteria from function type
		// TODO As a workaround we're ignoring null functionTypeRegistry. Remove this in the future
		if(functionTypeRegistry != null) {
			AbstractFunctionType<Function> functionType = functionTypeRegistry.getFunctionTypeByFunction(function);
			Map<String, Interest> tokenSelectionCriteriaFromFunctionType = functionType.getTokenSelectionCriteria(function);
			if(tokenSelectionCriteriaFromFunctionType!=null) {
				selectionCriteria.putAll(tokenSelectionCriteriaFromFunctionType);
			}			
		}

		// Criteria from function
		Map<String,String> tokenSelectionCriteriaFromFunction = function.getTokenSelectionCriteria();
		if(tokenSelectionCriteriaFromFunction!=null) {
			tokenSelectionCriteriaFromFunction.keySet().stream().forEach(key->selectionCriteria.put(key, new Interest(Pattern.compile(tokenSelectionCriteriaFromFunction.get(key)), true)));
		}

		// Criteria from bindings (Special variable "route_to_")
		addTokenSelectionCriteriaFromBindings(selectionCriteria, bindings);
		return selectionCriteria;
	}

	private static final String ROUTE_TO = "route_to_";

	private Map<String, Interest> addTokenSelectionCriteriaFromBindings(Map<String, Interest> addtionalSelectionCriteria, Map<String, Object> bindings) {
		bindings.forEach((k,v)->{
			if(v!=null){
				if(k.startsWith(ROUTE_TO)) {
					Pattern selectionPattern = Pattern.compile(v.toString());
					addtionalSelectionCriteria.put(k.replaceFirst(ROUTE_TO, ""), new Interest(selectionPattern, true));
				}
			}
		});
		return addtionalSelectionCriteria;
	}
}
