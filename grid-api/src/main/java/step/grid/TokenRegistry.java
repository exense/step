package step.grid;

import java.util.List;
import java.util.concurrent.TimeoutException;

import step.grid.tokenpool.Identity;
import step.grid.tokenpool.Token;

public interface TokenRegistry {

	TokenWrapper selectToken(Identity pretender, long matchTimeout, long noMatchTimeout)
			throws TimeoutException, InterruptedException;

	void returnToken(TokenWrapper object);

	List<Token<TokenWrapper>> getTokens();

}