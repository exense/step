package step.grid.client;

import step.grid.TokenWrapper;
import step.grid.io.OutputMessage;

public interface TokenLifecycleStrategy {
	
	public void afterTokenReleaseError(TokenLifecycleStrategyCallback callback, TokenWrapper tokenWrapper, Exception e);
	
	public void afterTokenReservationError(TokenLifecycleStrategyCallback callback, TokenWrapper tokenWrapper, Exception e);
	
	public void afterTokenCallError(TokenLifecycleStrategyCallback callback, TokenWrapper tokenWrapper, Exception e);
	
	public void afterTokenCall(TokenLifecycleStrategyCallback callback, TokenWrapper tokenWrapper, OutputMessage outputMessage);

}
