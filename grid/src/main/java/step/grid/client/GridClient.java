package step.grid.client;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.json.JsonObject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.grid.AgentRef;
import step.grid.Grid;
import step.grid.Token;
import step.grid.TokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.ObjectMapperResolver;
import step.grid.io.OutputMessage;
import step.grid.tokenpool.Identity;
import step.grid.tokenpool.Interest;

public class GridClient {
	
	private static final Logger logger = LoggerFactory.getLogger(GridClient.class);
	
	public static final String SELECTION_CRITERION_THREAD = "#THREADID#";
	
	private Grid adapterGrid;
	
	private Client client;
	
	private long noMatchExistsTimeout = 10000;
	
	private long matchExistsTimeout = 60000;
	
	public GridClient() {
		super();
	}

	public GridClient(Grid adapterGrid) {
		super();
		
		this.adapterGrid = adapterGrid;
		
		client = ClientBuilder.newClient();
		client.register(ObjectMapperResolver.class);
	}
	
	public OutputMessage processInput(String function, JsonObject argument, String handler) {
		return processInput(function, argument, handler, null, null);
	}
	
	public OutputMessage processInput(String function, JsonObject argument, String handler, Map<String, String> attributes, Map<String, Interest> interests) {
		
		TokenPretender tokenPretender = new TokenPretender(attributes, interests);
		TokenWrapper tokenWrapper = getAdapterToken(null, tokenPretender);
		Token token = tokenWrapper.getToken();
		
		AgentRef agent = adapterGrid.getAgentRefs().get(token.getAgentid());
		
		try {
			InputMessage message = new InputMessage();
			message.setArguments(argument);
			message.setFunction(function);
			message.setTokenId(token.getId());
			message.setHandler(handler);
			OutputMessage output = callAgent(agent, token, message);			
			token.getAttributes().put(SELECTION_CRITERION_THREAD, Long.toString(Thread.currentThread().getId()));
			return output;
		} finally {
			tokenWrapper.setCurrentOwner(null);
			returnAdapterToken(null, tokenWrapper);						
		}
	}
	
	private OutputMessage callAgent(AgentRef agentRef, Token token, InputMessage message) {
		// TODO get from config?
		int connectionTimeout = 3000;
		int callTimeout = 180000;
		
		String agentUrl = agentRef.getAgentUrl();
		
		try {			
			Entity<InputMessage> entity = Entity.entity(message, MediaType.APPLICATION_JSON);
			Response response = client.target(agentUrl + "/process").request().property(ClientProperties.READ_TIMEOUT, callTimeout)
					.property(ClientProperties.CONNECT_TIMEOUT, connectionTimeout).post(entity);
			//response.readEntity(String.class)
			OutputMessage output = response.readEntity(OutputMessage.class);
			return output;
		} catch (ProcessingException e) {
			throw e;
		}
	}

//	public void releaseSession(GridSession adapterSession) {
//		if(adapterSession!=null) {
//			for(TokenWrapper token:adapterSession.getAllTokens()) {
//				returnAdapterTokenToRegister(token);
//			}
//		}
//	}
//	
	
	private TokenWrapper getAdapterToken(GridSession adapterSession, Identity tokenPretender) {
		TokenWrapper token = null;
		if(adapterSession != null) {
			token = adapterSession.getToken(tokenPretender);
			if(token == null) {
				token = getAdapterTokenFromRegister(tokenPretender);
				adapterSession.putToken(tokenPretender, token);
			}
		} else {
			token = getAdapterTokenFromRegister(tokenPretender);
		}
		return token;
	}
	
	private TokenWrapper getAdapterTokenFromRegister(final Identity tokenPretender) {
		TokenWrapper adapterToken = null;
		try {
			adapterToken = adapterGrid.selectToken(tokenPretender, matchExistsTimeout, noMatchExistsTimeout);
		} catch (TimeoutException e) {
			String desc = "[attributes=" + tokenPretender.getAttributes() + ", selectionCriteria=" + tokenPretender.getInterests() + "]";
			throw new RuntimeException("Not able to find any available adapter matching " + desc);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return adapterToken;
	}
	
	private void returnAdapterToken(GridSession adapterSession, TokenWrapper adapterToken) {
		if(adapterSession==null) {
			returnAdapterTokenToRegister(adapterToken);
		}
	}

	private void returnAdapterTokenToRegister(TokenWrapper adapterToken) {
		adapterGrid.returnToken(adapterToken);		
	}
	
	public void close() {
		client.close();
	}
}
