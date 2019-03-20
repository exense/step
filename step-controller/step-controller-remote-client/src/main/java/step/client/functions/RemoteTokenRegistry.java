package step.client.functions;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;

import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.functions.services.GetTokenHandleParameter;
import step.grid.TokenWrapper;
import step.grid.tokenpool.Interest;
import step.grid.tokenpool.Token;
import step.grid.tokenpool.TokenRegistry;

public class RemoteTokenRegistry extends AbstractRemoteClient implements TokenRegistry {

	public RemoteTokenRegistry() {
		super();
	}

	public RemoteTokenRegistry(ControllerCredentials credentials) {
		super(credentials);
	}

	@Override
	// TODO matchTimeout and matchTimeout are currently ignored
	public TokenWrapper selectToken(Map<String, String> attributes, Map<String, Interest> interests, long matchTimeout,
			long noMatchTimeout) throws TimeoutException, InterruptedException {
		GetTokenHandleParameter parameter = new GetTokenHandleParameter();
		parameter.setAttributes(attributes);
		parameter.setInterests(interests);
		parameter.setCreateSession(false);
		
		Builder b = requestBuilder("/rest/functions/executor/tokens/select");
		Entity<GetTokenHandleParameter> entity = Entity.entity(parameter, MediaType.APPLICATION_JSON);
		return executeRequest(()->b.post(entity,TokenWrapper.class));
	}

	@Override
	public void returnToken(TokenWrapper object) {
		Builder b = requestBuilder("/rest/functions/executor/tokens/return");
		Entity<TokenWrapper> entity = Entity.entity(object, MediaType.APPLICATION_JSON);
		executeRequest(()->b.post(entity,TokenWrapper.class));
	}

	@Override
	public List<Token<TokenWrapper>> getTokens() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void markTokenAsFailing(String id, String errorMsg, Exception e) {
		throw new RuntimeException("Not implemented");
	}

}
