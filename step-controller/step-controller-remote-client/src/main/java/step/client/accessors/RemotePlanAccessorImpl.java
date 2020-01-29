package step.client.accessors;

import step.client.credentials.ControllerCredentials;
import step.core.plans.Plan;
import step.core.plans.PlanAccessor;

public class RemotePlanAccessorImpl extends AbstractRemoteCRUDAccessorImpl<Plan> implements PlanAccessor {

	public RemotePlanAccessorImpl() {
		super("/rest/plans/", Plan.class);
	}
	
	public RemotePlanAccessorImpl(ControllerCredentials credentials) {
		super(credentials, "/rest/plans/", Plan.class);
	}
}
