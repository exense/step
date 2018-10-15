package step.grid;

public class TokenHealth {

	protected TokenWrapperOwner tokenWrapperOwner;
	
	protected String errorMessage;
	
	protected Exception exception;
	
	public TokenWrapperOwner getTokenWrapperOwner() {
		return tokenWrapperOwner;
	}

	public void setTokenWrapperOwner(TokenWrapperOwner tokenWrapperOwner) {
		this.tokenWrapperOwner = tokenWrapperOwner;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public Exception getException() {
		return exception;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}
}
