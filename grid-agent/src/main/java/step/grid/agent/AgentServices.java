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
import java.util.ArrayList;
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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.grid.Token;
import step.grid.agent.tokenpool.AgentTokenPool;
import step.grid.agent.tokenpool.AgentTokenPool.InvalidTokenIdException;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.bootstrap.BootstrapManager;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

@Singleton
@Path("/")
public class AgentServices {

	private static final Logger logger = LoggerFactory.getLogger(AgentServices.class);

	@Inject
	Agent agent;

	final ExecutorService executor;

	AgentTokenPool tokenPool;

	BootstrapManager bootstrapManager;

	public AgentServices() {
		super();	
		executor = Executors.newCachedThreadPool();
	}

	@PostConstruct
	public void init() {
		tokenPool = agent.getTokenPool();
		bootstrapManager = new BootstrapManager(agent.getAgentTokenServices(), true);
	}

	class ExecutionContext {
		protected Thread t;
	}


	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/token/{id}/process")
	public OutputMessage process(@PathParam("id") String tokenId, final InputMessage message) {
		try {
			final AgentTokenWrapper tokenWrapper = tokenPool.getTokenForExecution(tokenId);
			if(tokenWrapper!=null) {
				// Now allowing token reuse
				//if(!tokenWrapper.isInUse()) {
				if(tokenWrapper.isInUse())
					logger.warn("Token with id=" + tokenWrapper.getUid() + " was already in use.");
				
				final ExecutionContext context = new ExecutionContext();
				tokenWrapper.setInUse(true);
				Future<OutputMessage> future = executor.submit(new Callable<OutputMessage>() {
					@Override
					public OutputMessage call() throws Exception {
						try {
							context.t = Thread.currentThread();
							agent.getAgentTokenServices().getApplicationContextBuilder().resetContext();
							return bootstrapManager.runBootstraped(tokenWrapper, message);
						} catch (Exception e) {
							return handleUnexpectedError(message, e);
						} finally {			
							tokenWrapper.setInUse(false);
						}
					}
				});

				try {
					OutputMessage output = future.get(message.getCallTimeout(), TimeUnit.MILLISECONDS);
					return output;
				} catch(TimeoutException e) {
					List<Attachment> attachments = new ArrayList<>();

					int i=0;
					boolean interruptionSucceeded = false;
					while(!interruptionSucceeded && i++<10) {
						interruptionSucceeded = tryInterruption(tokenWrapper, context, attachments);
					}

					future.cancel(true);

					if(!interruptionSucceeded) {
						return newErrorOutput("Timeout while processing request. WARNING: Request execution couldn't be interrupted. "
								+ "Subsequent calls to that token may fail!", attachments.toArray(new Attachment[0]));		
					} else {
						return newErrorOutput("Timeout while processing request. Request execution interrupted successfully.", attachments.toArray(new Attachment[0]));			
					}	
				}
				//} else {
				//	return newErrorOutput("Token " + tokenId + " already in use. The reason might be that a previous request timed out and couldn't be interrupted.");					
				//}
			} else {
				return newErrorOutput("No token found with id "+tokenId);
			}
		} catch(InvalidTokenIdException e) {
			return newErrorOutput("No token found with id '" + tokenId + "'");
		} catch (Exception e) {
			return handleUnexpectedError(message, e);
		}
	}

	private boolean tryInterruption(final AgentTokenWrapper tokenWrapper, final ExecutionContext context,
			List<Attachment> attachments) throws InterruptedException {
		if(tokenWrapper.isInUse()) {
			if(context.t!=null) {
				StackTraceElement[] stacktrace = context.t.getStackTrace();
				Attachment stacktraceAttachment = generateAttachmentForStacktrace("stacktrace_before_interruption.log",stacktrace);
				attachments.add(stacktraceAttachment);
				context.t.interrupt();
				Thread.sleep(10); 
				return !tokenWrapper.isInUse();			
			} else {
				return false;
			}
		} else {
			return true;
		}
	}

	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/token/{id}/reserve")
	public void reserveToken(@PathParam("id") String tokenId) throws InvalidTokenIdException {
		tokenPool.createTokenReservationSession(tokenId);
	}


	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/token/{id}/release")
	public void releaseToken(@PathParam("id") String tokenId) throws InvalidTokenIdException {
		tokenPool.closeTokenReservationSession(tokenId);
	}


	protected OutputMessage handleUnexpectedError(InputMessage inputMessage, Exception e) {
		StringBuilder message = new StringBuilder();
		message.append("Error in agent '").append(agent.getAgentUrl()).append("'");
		if(inputMessage!=null && inputMessage.getFunction()!=null) {
			message.append(" while executing '").append(inputMessage.getFunction()).append("'");
		}
		if(e.getMessage()!=null) {
			message.append(": ").append(e.getMessage()); 
		}
		OutputMessage output = newErrorOutput(message.toString());
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

	protected Attachment generateAttachmentForStacktrace(String attachmentName, StackTraceElement[] e) {
		Attachment attachment = new Attachment();	
		StringWriter str = new StringWriter();
		PrintWriter w = new PrintWriter(str);
		for (StackTraceElement traceElement : e)
			w.println("\tat " + traceElement);
		attachment.setName(attachmentName);
		attachment.setHexContent(AttachmentHelper.getHex(str.toString().getBytes()));
		return attachment;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/token/list")
	public List<Token> listTokens() {
		return agent.getTokens();
	}
}
