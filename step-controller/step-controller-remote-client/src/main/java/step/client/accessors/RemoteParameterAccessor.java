package step.client.accessors;

import step.client.credentials.ControllerCredentials;
import step.parameter.Parameter;
import step.parameter.ParameterAccessor;

public abstract class RemoteParameterAccessor extends AbstractRemoteCRUDAccessorImpl<Parameter> implements ParameterAccessor {

	public RemoteParameterAccessor(ControllerCredentials credentials, String path, Class<Parameter> entityClass) {
		super(credentials, path, entityClass);
	}

	public RemoteParameterAccessor(String path, Class<Parameter> entityClass) {
		super(path, entityClass);
	}
}
