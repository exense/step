package step.core.accessors;

import step.core.plans.Plan;
import step.core.plans.PlanAccessor;

public class PlanAccessorImpl extends AbstractCRUDAccessor<Plan> implements PlanAccessor {

	public PlanAccessorImpl(MongoClientSession clientSession) {
		super(clientSession, "plans", Plan.class);
	}

}
