package step.grid.agent;

import step.grid.agent.handler.MessageHandler;
import step.grid.agent.handler.MessageHandlerDelegate;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class TestTokenHandlerDelegator implements MessageHandler, MessageHandlerDelegate {

	MessageHandler delegate;
	
	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		OutputMessage output = delegate.handle(token, message);
		output.setError("Test error");
		return output;
	}

	@Override
	public void setDelegate(MessageHandler delegate) {
		this.delegate = delegate;
	}

}
