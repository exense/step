package step.client.accessors;

import step.client.credentials.ControllerCredentials;
import step.core.plans.Plan;

public class RemotePlanAccessorImpl extends RemotePlanAccessor {

	public RemotePlanAccessorImpl() {
		super("/rest/plans/", Plan.class);
	}
	
	public RemotePlanAccessorImpl(ControllerCredentials credentials) {
		super(credentials, "/rest/plans/", Plan.class);
	}
}
