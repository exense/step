package step.grid.agent;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import step.grid.Token;
import step.grid.agent.handler.TokenHandler;
import step.grid.agent.tokenpool.AgentTokenPool;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.agent.tokenpool.TokenSession;
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
					TokenSession session = tokenWrapper.getSession();
					TokenHandler tokenHandler = getTokenHandler(message, tokenWrapper);
					
					OutputMessage output = tokenHandler.handle(tokenWrapper.getToken(), session, message);
					return output;
				} finally {
					tokenPool.returnToken(tokenId);
				}
			} else {
				throw new RuntimeException("No token found with id "+tokenId);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/token/list")
	public List<Token> interrupt() {
		return agent.getTokens();
	}

	private TokenHandler getTokenHandler(InputMessage message, AgentTokenWrapper tokenWrapper) throws Exception {
		String handler;
		if(message.getHandler()!=null) {
			handler = message.getHandler();
		} else {
			String defaultHandler = tokenWrapper.getProperties().get("tokenhandler.default");
			if(defaultHandler!=null) {
				handler = defaultHandler;
			} else {
				throw new Exception("No handler found.");
			}
		}
		return agent.getHandlerPool().get(handler);
	}
}
