package step.client.accessors;

import step.client.credentials.ControllerCredentials;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;

public abstract class RemotePlanAccessor extends AbstractRemoteCRUDAccessorImpl<Plan> implements PlanAccessor {

	public RemotePlanAccessor(ControllerCredentials credentials, String path, Class<Plan> entityClass) {
		super(credentials, path, entityClass);
	}

	public RemotePlanAccessor(String path, Class<Plan> entityClass) {
		super(path, entityClass);
	}
}
