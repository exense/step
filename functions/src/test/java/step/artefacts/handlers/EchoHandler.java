package step.artefacts.handlers;

import step.grid.agent.handler.AbstractMessageHandler;
import step.grid.agent.handler.context.OutputMessageBuilder;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class EchoHandler extends AbstractMessageHandler {

	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		OutputMessageBuilder builder = new OutputMessageBuilder();
		builder.setPayloadJson(message.getArgument().toString());
		return builder.build();
	}
}
