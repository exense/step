package step.grid.agent.handler;

import java.util.Map;

import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public interface PropertyAwareMessageHandler extends MessageHandler {

	public OutputMessage handle(AgentTokenWrapper token, Map<String, String> properties, InputMessage message) throws Exception;
}
