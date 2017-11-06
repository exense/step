package step.grid.agent.handler;

import step.grid.agent.AgentTokenServices;

public interface AgentContextAware {

	public void init(AgentTokenServices agentTokenServices);
}
