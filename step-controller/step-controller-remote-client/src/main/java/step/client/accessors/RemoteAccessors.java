package step.client.accessors;

import step.client.credentials.ControllerCredentials;
import step.core.execution.model.ExecutionAccessor;
import step.core.plans.PlanAccessor;
import step.functions.accessor.FunctionAccessor;

public class RemoteAccessors {
	
	protected final ControllerCredentials credentials;

	public RemoteAccessors(ControllerCredentials credentials) {
		super();
		this.credentials = credentials;
	}

	public FunctionAccessor getFunctionAccessor() {
		return new RemoteFunctionAccessorImpl(credentials);
	}

	public PlanAccessor getPlanAccessor() {
		return new RemotePlanAccessorImpl(credentials);
	}
	
	public ExecutionAccessor getExecutionAccessor() {
		return new RemoteExecutionAccessor(credentials);
	}
	
}
