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
import java.util.function.Function;
import java.util.regex.Pattern;

import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import step.grid.AgentRef;
import step.grid.Grid;
import step.grid.GridFileService;
import step.grid.Token;
import step.grid.TokenRegistry;
import step.grid.TokenWrapper;
import step.grid.agent.AgentTokenServices;
import step.grid.agent.handler.MessageHandler;
import step.grid.agent.handler.TokenHandlerPool;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.filemanager.FileManagerClient;
import step.grid.io.InputMessage;
import step.grid.io.ObjectMapperResolver;
import step.grid.io.OutputMessage;
import step.grid.tokenpool.Identity;
import step.grid.tokenpool.Interest;

public class GridClient implements Closeable {
	
	private static final Logger logger = LoggerFactory.getLogger(GridClient.class);
	
	public static final String SELECTION_CRITERION_THREAD = "#THREADID#";
	
	private final GridFileService fileService;
	
	private final TokenRegistry tokenRegistry;
	
	private Client client;
	
	private long noMatchExistsTimeout = 10000;
	
	private long matchExistsTimeout = 60000;

	public GridClient(TokenRegistry tokenRegistry, GridFileService fileService) {
		super();
		
		this.tokenRegistry = tokenRegistry;
		this.fileService = fileService;
		
		client = ClientBuilder.newClient();
		client.register(ObjectMapperResolver.class);
		client.register(JacksonJsonProvider.class);
	}
	
	public TokenWrapper getLocalTokenHandle() {
		Token localToken = new Token();
		localToken.setId(UUID.randomUUID().toString());
		localToken.setAgentid(Grid.LOCAL_AGENT);		
		localToken.setAttributes(new HashMap<String, String>());
		localToken.setSelectionPatterns(new HashMap<String, Interest>());
		TokenWrapper tokenWrapper = new TokenWrapper(localToken, new AgentRef(Grid.LOCAL_AGENT, "localhost"));
		return tokenWrapper;
	}
	
	public TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests, boolean createSession) throws AgentCommunicationException {
		TokenPretender tokenPretender = new TokenPretender(attributes, interests);
		TokenWrapper tokenWrapper = getToken(tokenPretender);
		
		if(createSession) {
			try {
				reserveSession(tokenWrapper.getAgent(), tokenWrapper.getToken());			
				tokenWrapper.setHasSession(true);				
			} catch(AgentCommunicationException e) {
				logger.warn("Error while reserving session for token "+tokenWrapper.getID() +". Returning token to pool. "
						+ "Subsequent call to this token may fail or leaks may appear on the agent side.", e);
				returnTokenHandle(tokenWrapper);
				throw e;
			}
		}
		return tokenWrapper;
	}
	
	public void returnTokenHandle(TokenWrapper tokenWrapper) throws AgentCommunicationException {
		if(!tokenWrapper.getToken().getAgentid().equals(Grid.LOCAL_AGENT)) {
			tokenRegistry.returnToken(tokenWrapper);		
		}
		
		if(tokenWrapper.hasSession()) {
			//tokenWrapper.setHasSession(false);
			releaseSession(tokenWrapper.getAgent(),tokenWrapper.getToken());			
		}
	}
	
	public OutputMessage call(TokenWrapper tokenWrapper, String function, JsonObject argument, String handler, Map<String,String> properties, int callTimeout) throws Exception {
		Token token = tokenWrapper.getToken();
		
		AgentRef agent = tokenWrapper.getAgent();
		
		InputMessage message = new InputMessage();
		message.setArgument(argument);
		message.setFunction(function);
		message.setHandler(handler);
		message.setProperties(properties);
		message.setCallTimeout(callTimeout);
		
		OutputMessage output;
		if(token.isLocal()) {
			output = callLocalToken(token, message);
		} else {
			output = callAgent(agent, token, message);						
		}
		return output;
	}
	
	TokenHandlerPool localHandlerPool = new TokenHandlerPool(null);

	private OutputMessage callLocalToken(Token token, InputMessage message) throws Exception {
		OutputMessage output;
		MessageHandler h = localHandlerPool.get(message.getHandler());
		
		AgentTokenWrapper agentTokenWrapper = new AgentTokenWrapper(token);
		FileManagerClient fileManagerClient = new FileManagerClient() {
			@Override
			public File requestFile(String uid, long lastModified) {
				return fileService.getRegisteredFile(uid);
			}
		};
		agentTokenWrapper.setServices(new AgentTokenServices(fileManagerClient));
		output = h.handle(agentTokenWrapper, message);
		return output;
	}

	private void reserveSession(AgentRef agentRef, Token token) throws AgentCommunicationException {
		call(agentRef, token, "/reserve", builder->builder.get());
	}
	
	private static final int READ_TIMEOUT_OFFSET = 3000;
	
	private OutputMessage callAgent(AgentRef agentRef, Token token, InputMessage message) throws AgentCommunicationException {
		return (OutputMessage) call(agentRef, token, "/process", builder->{
			builder.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT_OFFSET+message.getCallTimeout());
			Entity<InputMessage> entity = Entity.entity(message, MediaType.APPLICATION_JSON);
			return builder.post(entity);
		}, response-> {
			return response.readEntity(OutputMessage.class);
		});
	}
	
	private void releaseSession(AgentRef agentRef, Token token) throws AgentCommunicationException {
		call(agentRef, token, "/release", builder->builder.get());
	}
	
	private void call(AgentRef agentRef, Token token, String cmd, Function<Builder, Response> f) throws AgentCommunicationException {
		call(agentRef, token, cmd, f, null);
	}
	
	private Object call(AgentRef agentRef, Token token, String cmd, Function<Builder, Response> f, Function<Response, Object> mapper) throws AgentCommunicationException {
		String agentUrl = agentRef.getAgentUrl();
		int connectionTimeout = READ_TIMEOUT_OFFSET;
		// TODO make this configurable
		int callTimeoutOffset = 10000;
		Builder builder =  client.target(agentUrl + "/token/" + token.getId() + cmd).request()
				.property(ClientProperties.READ_TIMEOUT, callTimeoutOffset).property(ClientProperties.CONNECT_TIMEOUT, connectionTimeout);
		
		Response response = null;
		try {
			try {
				response = f.apply(builder);				
			} catch(Exception e) {
				throw new AgentCommunicationException(agentRef, token, cmd, e);
			}
			if(!(response.getStatus()==204||response.getStatus()==200)) {
				String error = response.readEntity(String.class);
				throw new AgentCommunicationException(agentRef, token, cmd, error);
			} else {
				if(mapper!=null) {
					return mapper.apply(response);
				} else {
					return null;
				}
			}
		} finally {
			if(response!=null) {
				response.close();				
			}
		}
	}
	
	public static class AgentCommunicationException extends Exception {
	
		private static final long serialVersionUID = 4337204149079143691L;

		AgentRef agentRef;
		
		Token token;
		
		String cmd;
		
		String errorMessage;

		public AgentCommunicationException(AgentRef agentRef, Token token, String cmd, String errorMessage) {
			super();
			this.agentRef = agentRef;
			this.token = token;
			this.cmd = cmd;
			this.errorMessage = errorMessage;
		}
		
		public AgentCommunicationException(AgentRef agentRef, Token token, String cmd, Exception e) {
			super(e);
			this.errorMessage = e.getMessage();
			this.agentRef = agentRef;
			this.token = token;
			this.cmd = cmd;
		}

		@Override
		public String getMessage() {
			return "Error while calling agent "+agentRef+" to execute "+cmd+" on token "+token+": "+errorMessage;
		}
	}

	private TokenWrapper getToken(final Identity tokenPretender) {
		TokenWrapper adapterToken = null;
		try {
			addThreadIdInterest(tokenPretender);
			adapterToken = tokenRegistry.selectToken(tokenPretender, matchExistsTimeout, noMatchExistsTimeout);
		} catch (TimeoutException e) {
			StringBuilder interestList = new StringBuilder();
			if(tokenPretender.getInterests()!=null) {
				tokenPretender.getInterests().forEach((k,v)->{interestList.append(k+"="+v+" and ");});				
			}
			
			String desc = " selection criteria " + interestList.toString() + " accepting attributes " + tokenPretender.getAttributes();
			throw new RuntimeException("Not able to find any agent token matching " + desc);
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

	public String registerFile(File file) {
		return fileService.registerFile(file);
	}

	@Override
	public void close() {
		client.close();
	}
}
