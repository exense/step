package step.plugins.jmeter;

import step.core.dynamicbeans.DynamicValue;
import step.functions.Function;

public class JMeterFunction extends Function {

	DynamicValue<String> jmeterTestplan = new DynamicValue<>();

	public DynamicValue<String> getJmeterTestplan() {
		return jmeterTestplan;
	}

	public void setJmeterTestplan(DynamicValue<String> jmeterTestplan) {
		this.jmeterTestplan = jmeterTestplan;
	}
}
