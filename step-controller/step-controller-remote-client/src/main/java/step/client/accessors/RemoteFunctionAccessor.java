package step.client.accessors;

import step.client.credentials.ControllerCredentials;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;

public abstract class RemoteFunctionAccessor extends AbstractRemoteCRUDAccessorImpl<Function> implements FunctionAccessor {

	public RemoteFunctionAccessor(ControllerCredentials credentials, String path, Class<Function> entityClass) {
		super(credentials, path, entityClass);
	}

	public RemoteFunctionAccessor(String path, Class<Function> entityClass) {
		super(path, entityClass);
	}
}
