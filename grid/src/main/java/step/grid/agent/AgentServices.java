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
package step.grid.agent;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import step.grid.Token;
import step.grid.agent.handler.MessageHandler;
import step.grid.agent.tokenpool.AgentTokenPool;
import step.grid.agent.tokenpool.AgentTokenPool.TokenInUseException;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

@Singleton
@Path("/")
public class AgentServices {

	@Inject
	Agent agent;
	
	ExecutorService executor;
		
	public AgentServices() {
		super();
		
		executor = Executors.newCachedThreadPool();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/process")
	public OutputMessage process(final InputMessage message) {
		try {
			final String tokenId = message.getTokenId();
			final AgentTokenPool tokenPool = agent.getTokenPool();
			final AgentTokenWrapper tokenWrapper = tokenPool.getToken(tokenId);
			
			if(tokenWrapper!=null) {
				final MessageHandler tokenHandler = getTokenHandler(message, tokenWrapper);
				
				final Object terminationLock = new Object();
				
				Future<OutputMessage> future = executor.submit(new Callable<OutputMessage>() {
					@Override
					public OutputMessage call() throws Exception {
						try {
							OutputMessage output = tokenHandler.handle(tokenWrapper, message);
							return output;
						} finally {
							tokenPool.returnToken(tokenId);
							synchronized (terminationLock) {
								terminationLock.notify();
							}
						}
					}
				});
				
				try {
					OutputMessage output = future.get(message.getCallTimeout(), TimeUnit.MILLISECONDS);
					return output;
				} catch(TimeoutException e) {
					future.cancel(true);
					
					synchronized (terminationLock) {
						if(tokenWrapper.isInUse()) {
							terminationLock.wait(100);
						}
					}
					
					if(tokenWrapper.isInUse()) {
						return newErrorOutput("Timeout while processing message. WARNING: Message execution couldn't be interrupted and the token couldn't be returned to the pool. Subsequent calls to that token may fail!");		
					} else {
						return newErrorOutput("Timeout while processing message. Message execution interrupted successfully.");						
					}
				}
			} else {
				return newErrorOutput("No token found with id "+tokenId);
			}
		} catch(TokenInUseException e) {
			return newErrorOutput("Token " + e.getToken().getUid() + " already in use. This should never happen!");
		} catch (Exception e) {
			OutputMessage output = newErrorOutput("Unexpected error while processing message.");
			output.addAttachment(generateAttachmentForException(e));
			return output;
		}
	}
	
	protected OutputMessage newErrorOutput(String error) {
		OutputMessage output = new OutputMessage();
		output.setError(error);
		return output;
	}
	
	protected Attachment generateAttachmentForException(Throwable e) {
		Attachment attachment = new Attachment();	
		attachment.setName("exception.log");
		StringWriter w = new StringWriter();
		e.printStackTrace(new PrintWriter(w));
		attachment.setHexContent(AttachmentHelper.getHex(w.toString().getBytes()));
		return attachment;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/token/list")
	public List<Token> listTokens() {
		return agent.getTokens();
	}

	private MessageHandler getTokenHandler(InputMessage message, AgentTokenWrapper tokenWrapper) throws Exception {
		String handler;
		if(message.getHandler()!=null) {
			handler = message.getHandler();
		} else {
			String defaultHandler = tokenWrapper.getProperties()!=null?tokenWrapper.getProperties().get("tokenhandler.default"):null;
			if(defaultHandler!=null) {
				handler = defaultHandler;
			} else {
				throw new Exception("No handler found.");
			}
		}
		return agent.getHandlerPool().get(handler);
	}
}
