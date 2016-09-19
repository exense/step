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

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import step.grid.Token;
import step.grid.agent.handler.MessageHandler;
import step.grid.agent.tokenpool.AgentTokenPool;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

@Path("/")
public class AgentServices {

	@Inject
	Agent agent;
		
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/process")
	public OutputMessage process(InputMessage message) {
		try {
			String tokenId = message.getTokenId();
			
			AgentTokenPool tokenPool = agent.getTokenPool();
			AgentTokenWrapper tokenWrapper = tokenPool.getToken(tokenId);
			
			if(tokenWrapper!=null) {
				try {
					MessageHandler tokenHandler = getTokenHandler(message, tokenWrapper);
					
					OutputMessage output = tokenHandler.handle(tokenWrapper, message);
					return output;						
				} finally {
					tokenPool.returnToken(tokenId);
				}
			} else {
				throw new RuntimeException("No token found with id "+tokenId);
			}
		} catch (Exception e) {
			OutputMessage output = new OutputMessage();
			output.addAttachment(generateAttachmentForException(e));
			output.setError(e.getClass().getName()+":"+e.getMessage());
			return output;
		}
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
	public List<Token> interrupt() {
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
