package step.grid.client;

import step.grid.tokenpool.TokenRegistry;

public class TokenLifecycleStrategyCallback {

	private TokenRegistry tokenRegistry;
	
	private String tokenId;
	
	public TokenLifecycleStrategyCallback(TokenRegistry tokenRegistry, String tokenId) {
		super();
		this.tokenRegistry = tokenRegistry;
		this.tokenId = tokenId;
	}

	public void addTokenError(String errorMessage, Exception exception) {
		tokenRegistry.markTokenAsFailing(tokenId, errorMessage, exception);
	}

}
