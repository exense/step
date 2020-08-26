package step.client.accessors;

import step.client.credentials.ControllerCredentials;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;

public abstract class RemoteExecutionAccessor extends AbstractRemoteCRUDAccessorImpl<Execution> implements ExecutionAccessor {

	public RemoteExecutionAccessor(ControllerCredentials credentials, String path, Class<Execution> entityClass) {
		super(credentials, path, entityClass);
	}

	public RemoteExecutionAccessor(String path, Class<Execution> entityClass) {
		super(path, entityClass);
	}

}
