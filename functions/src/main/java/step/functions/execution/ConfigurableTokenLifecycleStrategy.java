package step.functions.execution;

import java.util.Set;

import step.grid.TokenWrapper;
import step.grid.client.TokenLifecycleStrategy;
import step.grid.client.TokenLifecycleStrategyCallback;
import step.grid.io.AgentErrorCode;
import step.grid.io.OutputMessage;

public class ConfigurableTokenLifecycleStrategy implements TokenLifecycleStrategy {

	private boolean addErrorOnTokenReleaseError = true;
	private boolean addErrorOnTokenReservationError = true;
	private boolean addErrorOnTokenCallError = true;
	private boolean addErrorOnAgentError = true;
	private Set<AgentErrorCode> concernedAgentErrors;
	
	
	public ConfigurableTokenLifecycleStrategy(boolean addErrorOnTokenReleaseError,
			boolean addErrorOnTokenReservationError, boolean addErrorOnTokenCallError, boolean addErrorOnAgentError,
			Set<AgentErrorCode> concernedAgentErrors) {
		super();
		this.addErrorOnTokenReleaseError = addErrorOnTokenReleaseError;
		this.addErrorOnTokenReservationError = addErrorOnTokenReservationError;
		this.addErrorOnTokenCallError = addErrorOnTokenCallError;
		this.addErrorOnAgentError = addErrorOnAgentError;
		this.concernedAgentErrors = concernedAgentErrors;
	}

	public boolean isAddErrorOnTokenReleaseError() {
		return addErrorOnTokenReleaseError;
	}

	public void setAddErrorOnTokenReleaseError(boolean addErrorOnTokenReleaseError) {
		this.addErrorOnTokenReleaseError = addErrorOnTokenReleaseError;
	}

	public boolean isAddErrorOnTokenReservationError() {
		return addErrorOnTokenReservationError;
	}

	public void setAddErrorOnTokenReservationError(boolean addErrorOnTokenReservationError) {
		this.addErrorOnTokenReservationError = addErrorOnTokenReservationError;
	}

	public boolean isAddErrorOnTokenCallError() {
		return addErrorOnTokenCallError;
	}

	public void setAddErrorOnTokenCallError(boolean addErrorOnTokenCallError) {
		this.addErrorOnTokenCallError = addErrorOnTokenCallError;
	}

	public boolean isAddErrorOnAgentError() {
		return addErrorOnAgentError;
	}

	public void setAddErrorOnAgentError(boolean addErrorOnAgentError) {
		this.addErrorOnAgentError = addErrorOnAgentError;
	}

	public Set<AgentErrorCode> getConcernedAgentErrors() {
		return concernedAgentErrors;
	}

	public void setConcernedAgentErrors(Set<AgentErrorCode> concernedAgentErrors) {
		this.concernedAgentErrors = concernedAgentErrors;
	}

	@Override
	public void afterTokenReleaseError(TokenLifecycleStrategyCallback callback, TokenWrapper tokenWrapper,
			Exception e) {
		if(addErrorOnTokenReleaseError) {
			callback.addTokenError("Error while releasing token",e);
		}
	}

	@Override
	public void afterTokenReservationError(TokenLifecycleStrategyCallback callback, TokenWrapper tokenWrapper,
			Exception e) {
		if(addErrorOnTokenReservationError) {
			callback.addTokenError("Error while reserving token",e);
		}
	}

	@Override
	public void afterTokenCallError(TokenLifecycleStrategyCallback callback, TokenWrapper tokenWrapper, Exception e) {
		if(addErrorOnTokenCallError) { 
			callback.addTokenError("Error while calling agent", e);
		}
	}

	@Override
	public void afterTokenCall(TokenLifecycleStrategyCallback callback, TokenWrapper tokenWrapper,
			OutputMessage outputMessage) {
		if(addErrorOnAgentError) {
			if(outputMessage.getAgentError()!=null) {
				if(concernedAgentErrors == null || concernedAgentErrors.isEmpty() || concernedAgentErrors.contains(outputMessage.getAgentError().getErrorCode())) {
					callback.addTokenError("Error while calling agent", null);
				}
			}
		}
	}
}
