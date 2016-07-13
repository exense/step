package step.grid.agent;

import step.grid.Token;
import step.grid.agent.handler.TokenHandler;
import step.grid.agent.tokenpool.TokenSession;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class TestTokenHandler implements TokenHandler {

	@Override
	public OutputMessage handle(Token token, TokenSession session, InputMessage message) {
		OutputMessage output = new OutputMessage();
		output.setPayload(message.getArgument());
		return output;
	}

}
