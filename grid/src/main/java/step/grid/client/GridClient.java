/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.grid.client;

import java.io.Closeable;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

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

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import step.grid.AgentRef;
import step.grid.Grid;
import step.grid.Token;
import step.grid.TokenWrapper;
import step.grid.agent.handler.MessageHandler;
import step.grid.agent.handler.TokenHandlerPool;
import step.grid.io.InputMessage;
import step.grid.io.ObjectMapperResolver;
import step.grid.io.OutputMessage;
import step.grid.tokenpool.Identity;
import step.grid.tokenpool.Interest;

public class GridClient implements Closeable {
	
	private static final Logger logger = LoggerFactory.getLogger(GridClient.class);
	
	public static final String SELECTION_CRITERION_THREAD = "#THREADID#";
	
	private final Grid adapterGrid;
	
	private Client client;
	
	private long noMatchExistsTimeout = 10000;
	
	private long matchExistsTimeout = 60000;

	public GridClient(Grid adapterGrid) {
		super();
		
		this.adapterGrid = adapterGrid;
		
		client = ClientBuilder.newClient();
		client.register(ObjectMapperResolver.class);
		client.register(JacksonJsonProvider.class);
	}
	
	private OutputMessage processInput(TokenWrapper tokenWrapper, String function, JsonObject argument, String handler, Map<String,String> properties, int callTimeout) throws Exception {
		Token token = tokenWrapper.getToken();
		
		AgentRef agent = tokenWrapper.getAgent();
		
		InputMessage message = new InputMessage();
		message.setArgument(argument);
		message.setFunction(function);
		message.setTokenId(token.getId());
		message.setHandler(handler);
		message.setProperties(properties);
		message.setCallTimeout(callTimeout);
		
		OutputMessage output;
		if(token.isLocal()) {
			output = callLocalToken(message);
		} else {
			output = callAgent(agent, token, message);						
		}
		return output;
	}

	private OutputMessage callLocalToken(InputMessage message) throws Exception {
		OutputMessage output;
		TokenHandlerPool p = new TokenHandlerPool();
		MessageHandler h = p.get(message.getHandler());
		output = h.handle(null, message);
		return output;
	}

	public TokenHandle getLocalToken() {
		Token localToken = new Token();
		localToken.setId(UUID.randomUUID().toString());
		localToken.setAgentid(Grid.LOCAL_AGENT);		
		localToken.setAttributes(new HashMap<String, String>());
		localToken.setSelectionPatterns(new HashMap<String, Interest>());
		TokenWrapper tokenWrapper = new TokenWrapper(localToken, new AgentRef(Grid.LOCAL_AGENT, "localhost"));
		return new TokenHandle(tokenWrapper);
	}
	
	public TokenHandle getToken() {
		TokenPretender tokenPretender = new TokenPretender(null, null);
		TokenWrapper tokenWrapper = getToken(tokenPretender);
		return new TokenHandle(tokenWrapper);
	}
	
	public TokenHandle getToken(Map<String, String> attributes, Map<String, Interest> interests) {
		TokenPretender tokenPretender = new TokenPretender(attributes, interests);
		TokenWrapper tokenWrapper = getToken(tokenPretender);
		return new TokenHandle(tokenWrapper);
	}
	
	public class TokenHandle {
		
		TokenWrapper token;
		
		String handler = null;
		
		Map<String,String> properties = new HashMap<>();
		
		int callTimeout = 180000;
		
		private TokenHandle(TokenWrapper token) {
			super();
			this.token = token;
		}

		public TokenHandle setCallTimeout(int callTimeout) {
			this.callTimeout = callTimeout;
			return this;
		}

		public TokenHandle setHandler(String handler) {
			this.handler = handler;
			return this;
		}
		
		public TokenHandle addProperties(Map<String, String> properties) {
			if(properties!=null) {
				this.properties.putAll(properties);
			}
			return this;
		}

		public void setCurrentOwner(Object currentOwner) {
			token.setCurrentOwner(currentOwner);
		}

		public OutputMessage process(String function, JsonObject argument) throws Exception {
			return processInput(token, function, argument, handler, properties, callTimeout);
		}
		
		public OutputMessage processAndRelease(String function, JsonObject argument) throws Exception {
			try {
				return processInput(token, function, argument, handler, properties, callTimeout);
			} finally {
				release();
			}
		}
		
		public TokenWrapper getToken() {
			return token;
		}
		
		public void release() {
			if(!token.getToken().getAgentid().equals(Grid.LOCAL_AGENT)) {
				returnAdapterTokenToRegister(token);				
			}
		}
	}
	
	private OutputMessage callAgent(AgentRef agentRef, Token token, InputMessage message) throws Exception {
		// TODO get from config?
		int connectionTimeout = 3000;
		int callTimeoutOffset = 3000;
		
		String agentUrl = agentRef.getAgentUrl();
		
		try {			
			Entity<InputMessage> entity = Entity.entity(message, MediaType.APPLICATION_JSON);
			Response response = client.target(agentUrl + "/process").request().property(ClientProperties.READ_TIMEOUT, message.getCallTimeout()+callTimeoutOffset)
					.property(ClientProperties.CONNECT_TIMEOUT, connectionTimeout).post(entity);
			try {
				if(response.getStatus()==200) {
					OutputMessage output = response.readEntity(OutputMessage.class);
					return output;				
				} else {
					String error = response.readEntity(String.class);
					throw new Exception("Error while calling agent with ref " + agentRef.toString()+ ". HTTP Response: "+error);
				}
			} finally {
				response.close();
			}
		} catch (ProcessingException e) {
			throw e;
		}
	}
	
	private TokenWrapper getToken(final Identity tokenPretender) {
		TokenWrapper adapterToken = null;
		try {
			addThreadIdInterest(tokenPretender);
			adapterToken = adapterGrid.selectToken(tokenPretender, matchExistsTimeout, noMatchExistsTimeout);
		} catch (TimeoutException e) {
			String desc = "[attributes=" + tokenPretender.getAttributes() + ", selectionCriteria=" + tokenPretender.getInterests() + "]";
			throw new RuntimeException("Not able to find any available adapter matching " + desc);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		markTokenWithThreadId(adapterToken);
		return adapterToken;
	}

	private void markTokenWithThreadId(TokenWrapper adapterToken) {
		if(adapterToken.getAttributes()!=null) {
			adapterToken.getAttributes().put(SELECTION_CRITERION_THREAD, Long.toString(Thread.currentThread().getId()));			
		}
	}

	private void addThreadIdInterest(final Identity tokenPretender) {
		if(tokenPretender.getInterests()!=null) {
			tokenPretender.getInterests().put(SELECTION_CRITERION_THREAD, new Interest(Pattern.compile("^"+Long.toString(Thread.currentThread().getId())+"$"), false));				
		}
	}

	private void returnAdapterTokenToRegister(TokenWrapper adapterToken) {
		adapterGrid.returnToken(adapterToken);		
	}
	
	public String registerFile(File file) {
		return adapterGrid.registerFile(file);
	}

	@Override
	public void close() {
		client.close();
	}
}
