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
	
	private OutputMessage processInput(TokenWrapper tokenWrapper, String function, JsonObject argument, String handler, Map<String,String> properties) throws Exception {
		Token token = tokenWrapper.getToken();
		
		AgentRef agent = adapterGrid.getAgentRefs().get(token.getAgentid());
		
		InputMessage message = new InputMessage();
		message.setArgument(argument);
		message.setFunction(function);
		message.setTokenId(token.getId());
		message.setHandler(handler);
		message.setProperties(properties);
		OutputMessage output = callAgent(agent, token, message);			
		token.getAttributes().put(SELECTION_CRITERION_THREAD, Long.toString(Thread.currentThread().getId()));
		return output;
	}

	public TokenFacade getToken() {
		TokenPretender tokenPretender = new TokenPretender(null, null);
		TokenWrapper tokenWrapper = getToken(tokenPretender);
		return new TokenFacade(tokenWrapper);
	}
	
	public TokenFacade getToken(Map<String, String> attributes, Map<String, Interest> interests) {
		TokenPretender tokenPretender = new TokenPretender(attributes, interests);
		TokenWrapper tokenWrapper = getToken(tokenPretender);
		return new TokenFacade(tokenWrapper);
	}
	
	public class TokenFacade {
		
		TokenWrapper token;
		
		public TokenFacade(TokenWrapper token) {
			super();
			this.token = token;
		}

		public OutputMessage process(String function, JsonObject argument) throws Exception {
			return processInput(token, function, argument, null, null);
		}
		
		public OutputMessage process(String function, JsonObject argument, String handler) throws Exception {
			return processInput(token, function, argument, handler, null);
		}
		
		public OutputMessage process(String function, JsonObject argument, String handler, Map<String,String> properties) throws Exception {
			return processInput(token, function, argument, handler, properties);
		}
		
		public OutputMessage processAndRelease(String function, JsonObject argument, String handler, Map<String,String> properties) throws Exception {
			try {
				return processInput(token, function, argument, handler, properties);
			} finally {
				release();
			}
		}
		
		public OutputMessage processAndRelease(String function, JsonObject argument, String handler) throws Exception {
			try {
				return processInput(token, function, argument, handler, null);
			} finally {
				release();
			}
		}
		
		public TokenWrapper getToken() {
			return token;
		}
		
		public void release() {
			returnAdapterTokenToRegister(token);
		}
	}
	
	private OutputMessage callAgent(AgentRef agentRef, Token token, InputMessage message) throws Exception {
		// TODO get from config?
		int connectionTimeout = 3000;
		int callTimeout = 180000;
		
		String agentUrl = agentRef.getAgentUrl();
		
		try {			
			Entity<InputMessage> entity = Entity.entity(message, MediaType.APPLICATION_JSON);
			Response response = client.target(agentUrl + "/process").request().property(ClientProperties.READ_TIMEOUT, callTimeout)
					.property(ClientProperties.CONNECT_TIMEOUT, connectionTimeout).post(entity);
			if(response.getStatus()==200) {
				OutputMessage output = response.readEntity(OutputMessage.class);
				return output;				
			} else {
				String error = response.readEntity(String.class);
				throw new Exception("Error while calling agent with ref " + agentRef.toString()+ ". HTTP Response: "+error);
			}
		} catch (ProcessingException e) {
			throw e;
		}
	}
	
	private TokenWrapper getToken(final Identity tokenPretender) {
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

	private void returnAdapterTokenToRegister(TokenWrapper adapterToken) {
		adapterGrid.returnToken(adapterToken);		
	}
	
	public void close() {
		client.close();
	}
}
