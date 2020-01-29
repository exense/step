package step.plugins.functions.types;

import step.functions.Function;

public class CompositeFunction extends Function {

	protected String planId;
	
	public CompositeFunction() {
		super();
		executeLocally = true;
	}

	public String getPlanId() {
		return planId;
	}

	public void setPlanId(String planId) {
		this.planId = planId;
	}
}
