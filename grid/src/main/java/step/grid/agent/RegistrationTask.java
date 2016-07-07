package step.grid.agent;

import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.grid.AgentRef;
import step.grid.RegistrationMessage;

public class RegistrationTask extends TimerTask {
	
	private static final Logger logger = LoggerFactory.getLogger(RegistrationTask.class);
		
	private final Agent agent;
	
	private final RegistrationClient client;
	
	private boolean interrupted;

	public RegistrationTask(Agent agent) {
		super();
		
		this.agent = agent;
		this.client = new RegistrationClient(agent.getGridHost());
	}

	@Override
	public void run() {
		if(!interrupted) {
			try {		
				RegistrationMessage message = new RegistrationMessage(new AgentRef(agent.getId(), agent.getAgentUrl()), agent.getTokens());
				client.sendRegistrationMessage(message);
			} catch (Exception e) {
				logger.error("An unexpected error occurred while registering the adapter.",e);
			}
		}
	}
	
	protected void interrupt() {
		interrupted = true;
	}
	
	protected void resume() {
		interrupted = false;
	}
	
	protected void unregister() {
		try {
			agent.getTokens().forEach(t->client.unregisterToken(t));
		} catch (Exception e) {
			logger.error("An error occurred while unregistering the adapter.",e);
		}
	}
	
	protected void destroy() {
		client.close();
	}

}
