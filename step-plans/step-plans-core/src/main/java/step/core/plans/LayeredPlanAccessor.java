package step.core.plans;

import java.util.List;

import step.core.accessors.LayeredCRUDAccessor;

public class LayeredPlanAccessor extends LayeredCRUDAccessor<Plan> implements PlanAccessor {

	public LayeredPlanAccessor() {
		super();
	}

	public LayeredPlanAccessor(List<PlanAccessor> accessors) {
		super(accessors);
	}

}
