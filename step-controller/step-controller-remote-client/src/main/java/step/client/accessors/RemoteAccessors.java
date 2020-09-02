package step.client.accessors;

import step.client.credentials.ControllerCredentials;

public class RemoteAccessors {
	
	protected final ControllerCredentials credentials;

	public RemoteAccessors(ControllerCredentials credentials) {
		super();
		this.credentials = credentials;
	}

	public RemoteFunctionAccessor getFunctionAccessor() {
		return new RemoteFunctionAccessorImpl(credentials);
	}

	public RemotePlanAccessor getPlanAccessor() {
		return new RemotePlanAccessorImpl(credentials);
	}
	
	public RemoteExecutionAccessor getExecutionAccessor() {
		return new RemoteExecutionAccessorImpl(credentials);
	}

	public RemoteParameterAccessor getParameterAccessor() {
		return new RemoteParameterAccessorImpl(credentials);
	}
}
