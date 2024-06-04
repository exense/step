package step.functions.handler;

import step.grid.agent.handler.AbstractMessageHandler;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class TestMessageHandler extends AbstractMessageHandler {

    @Override
    public OutputMessage handle(AgentTokenWrapper agentTokenWrapper, InputMessage inputMessage) throws Exception {
        OutputMessage outputMessage = new OutputMessage();
        outputMessage.setPayload(inputMessage.getPayload());
        return outputMessage;
    }
}
