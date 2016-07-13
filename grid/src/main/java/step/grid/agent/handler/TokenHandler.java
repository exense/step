package step.grid.agent.handler;

import step.grid.Token;
import step.grid.agent.tokenpool.TokenSession;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public interface TokenHandler {

	public OutputMessage handle(Token token, TokenSession session, InputMessage message) throws Exception;
}
