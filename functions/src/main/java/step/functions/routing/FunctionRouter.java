package step.functions.routing;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import step.artefacts.CallFunction;
import step.artefacts.handlers.FunctionGroupHandler.FunctionGroupContext;
import step.artefacts.handlers.TokenSelectorHelper;
import step.common.managedoperations.OperationManager;
import step.core.dynamicbeans.DynamicJsonObjectResolver;
import step.functions.Function;
import step.functions.execution.FunctionExecutionService;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.grid.TokenWrapper;
import step.grid.client.GridClient.AgentCommunicationException;
import step.grid.tokenpool.Interest;

public class FunctionRouter {

	protected final TokenSelectorHelper tokenSelectorHelper;
	
	protected final FunctionExecutionService functionExecutionService;
	
	protected final FunctionTypeRegistry functionTypeRegistry;

	public FunctionRouter(FunctionExecutionService functionClient, FunctionTypeRegistry functionTypeRegistry, DynamicJsonObjectResolver dynamicJsonObjectResolver) {
		super();
		this.functionExecutionService = functionClient;
		this.functionTypeRegistry = functionTypeRegistry;
		this.tokenSelectorHelper = new TokenSelectorHelper(dynamicJsonObjectResolver);
	}

	public TokenWrapper selectToken(CallFunction callFunction, Function function, FunctionGroupContext functionGroupContext, Map<String, Object> bindings) throws AgentCommunicationException {
		TokenWrapper token;
		if(function.requiresLocalExecution()) {
			// The function requires a local execution => get a local token
			token = functionExecutionService.getLocalTokenHandle();
		} else {
			if(functionGroupContext!=null) {
				if(functionGroupContext.getToken()!=null) {
					// Token already present in context => reusing it
					token = functionGroupContext.getToken();
				} else {
					// Token not present in context => select a token and create a new agent session
					Map<String, Interest> selectionCriteria = buildSelectionCriteriaMap(callFunction, function,	functionGroupContext, bindings);
					token = selectToken(selectionCriteria, true);
					// attach the token to the function group context
					functionGroupContext.setToken(token);
				}
			} else {
				// No FunctionGroupContext. Simply select a token without creating an agent session
				Map<String, Interest> selectionCriteria = buildSelectionCriteriaMap(callFunction, function,	functionGroupContext, bindings);
				token = selectToken(selectionCriteria, false);
			}
		}
		return token;
	}

	private TokenWrapper selectToken(Map<String, Interest> selectionCriteria, boolean createSession) throws AgentCommunicationException {		
		Map<String, String> pretenderAttributes = new HashMap<>();
		
		TokenWrapper token;
		OperationManager.getInstance().enter("Token selection", selectionCriteria);
		try {
			token = functionExecutionService.getTokenHandle(pretenderAttributes, selectionCriteria, createSession);
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
			if(k.startsWith(ROUTE_TO)) {
				Pattern selectionPattern = Pattern.compile(v.toString());
				addtionalSelectionCriteria.put(k.replaceFirst(ROUTE_TO, ""), new Interest(selectionPattern, true));
			}
		});
		return addtionalSelectionCriteria;
	}
}
