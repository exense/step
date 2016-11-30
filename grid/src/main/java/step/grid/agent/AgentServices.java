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

import javax.annotation.PostConstruct;
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
	
	final ExecutorService executor;
	
	AgentTokenPool tokenPool;
		
	public AgentServices() {
		super();	
		executor = Executors.newCachedThreadPool();
	}
	
	@PostConstruct
	public void init() {
		tokenPool = agent.getTokenPool();
	}

	class ExecutionContext {
		protected Thread t;
		protected boolean running = true;
	}
	
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/process")
	public OutputMessage process(final InputMessage message) {
		try {
			final String tokenId = message.getTokenId();
			final AgentTokenWrapper tokenWrapper = tokenPool.getToken(tokenId);
			if(tokenWrapper!=null) {				
				final ExecutionContext context = new ExecutionContext();
				Future<OutputMessage> future = executor.submit(new Callable<OutputMessage>() {
					@Override
					public OutputMessage call() throws Exception {
						try {
							context.t = Thread.currentThread();
							MessageHandler tokenHandler = getTokenHandler(message, tokenWrapper);
							OutputMessage output = tokenHandler.handle(tokenWrapper, message);
							return output;
						} catch (Exception e) {
							return handleUnexpectedError(e);
						} finally {
							tokenPool.returnToken(tokenId);
							synchronized (context) {
								context.running = false;								
								context.notify();
							}
						}
					}
				});
				
				try {
					OutputMessage output = future.get(message.getCallTimeout(), TimeUnit.MILLISECONDS);
					return output;
				} catch(TimeoutException e) {
					Attachment stacktraceAttachment = null;
					
					synchronized (context) {
						if(context.running && context.t!=null) {
							StackTraceElement[] stacktrace = context.t.getStackTrace();
							stacktraceAttachment = generateAttachmentForStacktrace(stacktrace);							
						}
					}
					
					future.cancel(true);
					
					synchronized (context) {
						if(context.running) {
							context.wait(100); 
						}
					}
					
					if(context.running) {
						return newErrorOutput("Timeout while processing request. WARNING: Request execution couldn't be interrupted and the token couldn't be returned to the pool. "
								+ "Subsequent calls to that token may fail!", stacktraceAttachment);		
					} else {
						return newErrorOutput("Timeout while processing request. Request execution interrupted successfully.", stacktraceAttachment);			
					}						
					
				}
			} else {
				return newErrorOutput("No token found with id "+tokenId);
			}
		} catch(TokenInUseException e) {
			return newErrorOutput("Token " + e.getToken().getUid() + " already in use. This should never happen!");
		} catch (Exception e) {
			return handleUnexpectedError(e);
		}
	}
	

	protected OutputMessage handleUnexpectedError(Exception e) {
		String message = "Error while processing message";
		if(e.getMessage()!=null) {
			message += ": "+e.getMessage(); 
		}
		OutputMessage output = newErrorOutput(message);
		output.addAttachment(generateAttachmentForException(e));
		return output;
	}
	
	protected OutputMessage newErrorOutput(String error, Attachment...attachments) {
		OutputMessage output = new OutputMessage();
		output.setError(error);
		if(attachments!=null) {
			for (Attachment attachment : attachments) {
				output.addAttachment(attachment);			
			}
		}
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
	
	protected Attachment generateAttachmentForStacktrace(StackTraceElement[] e) {
		Attachment attachment = new Attachment();	
		attachment.setName("stacktrace.log");
		StringWriter str = new StringWriter();
		PrintWriter w = new PrintWriter(str);
		for (StackTraceElement traceElement : e)
			w.println("\tat " + traceElement);
		attachment.setHexContent(AttachmentHelper.getHex(str.toString().getBytes()));
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
