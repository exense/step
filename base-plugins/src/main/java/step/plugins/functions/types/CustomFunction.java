package step.plugins.functions.types;

import step.core.dynamicbeans.DynamicValue;
import step.functions.Function;

public class CustomFunction extends Function {

	DynamicValue<String> handlerChain = new DynamicValue<>();

	public DynamicValue<String> getHandlerChain() {
		return handlerChain;
	}

	public void setHandlerChain(DynamicValue<String> handlerChain) {
		this.handlerChain = handlerChain;
	}
}
