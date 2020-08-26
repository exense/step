package step.client.functions;

import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.functions.manager.FunctionManager;

public abstract class RemoteFunctionManager extends AbstractRemoteClient implements FunctionManager {

	public RemoteFunctionManager(ControllerCredentials credentials) {
		super(credentials);
	}

	public RemoteFunctionManager() {
		super();
	}
}
