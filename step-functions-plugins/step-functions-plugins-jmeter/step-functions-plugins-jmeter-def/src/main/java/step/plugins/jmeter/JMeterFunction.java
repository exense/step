package step.plugins.jmeter;

import step.core.dynamicbeans.DynamicValue;
import step.core.entities.EntityManager;
import step.core.entities.EntityReference;
import step.functions.Function;

public class JMeterFunction extends Function {

	DynamicValue<String> jmeterTestplan = new DynamicValue<>();

	@EntityReference(type=EntityManager.resources)
	public DynamicValue<String> getJmeterTestplan() {
		return jmeterTestplan;
	}

	public void setJmeterTestplan(DynamicValue<String> jmeterTestplan) {
		this.jmeterTestplan = jmeterTestplan;
	}
}
