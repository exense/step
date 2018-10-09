package step.grid.tokenpool;

import java.util.List;
import java.util.concurrent.TimeoutException;

import step.grid.TokenWrapper;


public interface TokenRegistry {

	TokenWrapper selectToken(Identity pretender, long matchTimeout, long noMatchTimeout)
			throws TimeoutException, InterruptedException;

	void returnToken(TokenWrapper object);

	List<step.grid.tokenpool.Token<TokenWrapper>> getTokens();
	
	void markTokenAsFailing(String tokenId, String errorMessage, Exception e);

}