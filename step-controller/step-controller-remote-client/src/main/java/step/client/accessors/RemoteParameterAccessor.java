package step.client.accessors;

import step.client.credentials.ControllerCredentials;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.plugins.parametermanager.Parameter;
import step.plugins.parametermanager.ParameterAccessor;

public abstract class RemoteParameterAccessor extends AbstractRemoteCRUDAccessorImpl<Parameter> implements ParameterAccessor {

	public RemoteParameterAccessor(ControllerCredentials credentials, String path, Class<Parameter> entityClass) {
		super(credentials, path, entityClass);
	}

	public RemoteParameterAccessor(String path, Class<Parameter> entityClass) {
		super(path, entityClass);
	}
}
