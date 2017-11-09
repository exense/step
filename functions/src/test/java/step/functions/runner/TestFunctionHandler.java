package step.functions.runner;

import step.grid.agent.handler.AbstractMessageHandler;
import step.grid.agent.handler.context.OutputMessageBuilder;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class TestFunctionHandler extends AbstractMessageHandler {

	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		OutputMessageBuilder o = new OutputMessageBuilder();
		o.add("mous", "tache");
		return o.build();
	}

}
