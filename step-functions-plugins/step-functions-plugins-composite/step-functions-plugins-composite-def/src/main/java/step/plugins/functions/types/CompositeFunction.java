package step.plugins.functions.types;

import step.core.entities.EntityManager;
import step.core.entities.EntityReference;
import step.functions.Function;

public class CompositeFunction extends Function {

	protected String planId;
	
	public CompositeFunction() {
		super();
		executeLocally = true;
	}

	@EntityReference(type=EntityManager.plans)
	public String getPlanId() {
		return planId;
	}

	public void setPlanId(String planId) {
		this.planId = planId;
	}
}
