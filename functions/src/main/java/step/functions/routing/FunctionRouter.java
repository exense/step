package step.functions.routing;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import step.artefacts.handlers.FunctionGroupHandler.FunctionGroupContext;
import step.common.managedoperations.OperationManager;
import step.functions.Function;
import step.functions.FunctionClient;
import step.functions.FunctionExecutionService;
import step.functions.type.AbstractFunctionType;
import step.grid.TokenWrapper;
import step.grid.client.GridClient.AgentCommunicationException;
import step.grid.tokenpool.Interest;

public class FunctionRouter {

	protected final FunctionClient functionClient;

	public FunctionRouter(FunctionClient functionClient) {
		super();
		this.functionClient = functionClient;
	}

	public TokenWrapper selectToken(Function function, FunctionGroupContext functionGroupContext, Map<String, Interest> additionalSelectionCriteria) throws AgentCommunicationException {
		TokenWrapper token;
		if(function.requiresLocalExecution()) {
			// The function requires a local execution => get a local token
			token = functionClient.getLocalTokenHandle();
		} else {
			if(functionGroupContext!=null) {
				if(functionGroupContext.getToken()!=null) {
					// Token already present in context => reusing it
					token = functionGroupContext.getToken();
				} else {
					// Token not present in context => select one using the selection criteria of the context + those specified in additionalSelectionCriteria
					Map<String, Interest> allAdditionalSelectionCriteria = new HashMap<>();
					if(additionalSelectionCriteria!=null) {
						allAdditionalSelectionCriteria.putAll(additionalSelectionCriteria);
					}
					if(functionGroupContext.getAdditionalSelectionCriteria()!=null) {
						allAdditionalSelectionCriteria.putAll(functionGroupContext.getAdditionalSelectionCriteria());
					}
					token = selectToken(function, allAdditionalSelectionCriteria, true);
					functionGroupContext.setToken(token);
				}
			} else {
				// No FunctionGroupContext. Simply select a token without creating an agent session
				token = selectToken(function, additionalSelectionCriteria, false);
			}
		}
		return token;
	}

	private TokenWrapper selectToken(Function function, Map<String, Interest> additionalSelectionCriteria, boolean createSession) throws AgentCommunicationException {
		TokenWrapper token;
		Map<String, Interest> selectionCriteria = new HashMap<>();
		if(additionalSelectionCriteria!=null) {
			selectionCriteria.putAll(additionalSelectionCriteria);						
		}
		
		AbstractFunctionType<Function> functionType = functionClient.getFunctionTypeByFunction(function);
		Map<String, Interest> tokenSelectionCriteriaFromFunctionType = functionType.getTokenSelectionCriteria(function);
		if(tokenSelectionCriteriaFromFunctionType!=null) {
			selectionCriteria.putAll(tokenSelectionCriteriaFromFunctionType);
		}
		
		Map<String,String> tokenSelectionCriteriaFromFunction = function.getTokenSelectionCriteria();
		if(tokenSelectionCriteriaFromFunction!=null) {
			tokenSelectionCriteriaFromFunction.keySet().stream().forEach(key->selectionCriteria.put(key, new Interest(Pattern.compile(tokenSelectionCriteriaFromFunction.get(key)), true)));
		}
		
		Map<String, String> pretenderAttributes = new HashMap<>();
		//bindings.forEach((key,value)->{pretenderAttributes.put(key, value.toString());});
		
		OperationManager.getInstance().enter("Token selection", selectionCriteria);
		try {
			token = functionClient.getTokenHandle(pretenderAttributes, selectionCriteria, createSession);
		} finally {
			OperationManager.getInstance().exit();					
		}
		return token;
	}
}
