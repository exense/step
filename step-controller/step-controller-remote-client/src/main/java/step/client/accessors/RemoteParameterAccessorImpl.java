package step.client.accessors;

import step.client.credentials.ControllerCredentials;
import step.plugins.parametermanager.Parameter;

public class RemoteParameterAccessorImpl extends RemoteParameterAccessor {

	public RemoteParameterAccessorImpl() {
		super("/rest/parameters/", Parameter.class);
	}

	public RemoteParameterAccessorImpl(ControllerCredentials credentials) {
		super(credentials, "/rest/parameters/", Parameter.class);
	}
}
