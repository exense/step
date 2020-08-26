package step.client.accessors;

import step.client.credentials.ControllerCredentials;
import step.functions.Function;

public class RemoteFunctionAccessorImpl extends RemoteFunctionAccessor {

	public RemoteFunctionAccessorImpl() {
		super("/rest/functions/", Function.class);
	}
	
	public RemoteFunctionAccessorImpl(ControllerCredentials credentials) {
		super(credentials, "/rest/functions/", Function.class);
	}
}
