package step.grid.agent.handler;

import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public interface MessageHandler {

	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception;
}
