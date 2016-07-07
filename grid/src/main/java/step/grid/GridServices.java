package step.grid;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

@Path("/grid")
public class GridServices {

	@Inject
	Grid grid;

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/register")
	public void register(RegistrationMessage message) {
		AgentRef agentRef = message.getAgentRef();
		grid.getAgentRefs().putOrTouch(agentRef.getAgentId(), agentRef);
		for (Token token : message.getTokens()) {
			grid.getTokenPool().offerToken(new TokenWrapper(token));			
		}
	}
}
