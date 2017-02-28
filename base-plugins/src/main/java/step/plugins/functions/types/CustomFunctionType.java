package step.plugins.functions.types;

import java.util.Map;

import step.functions.type.AbstractFunctionType;

public class CustomFunctionType extends AbstractFunctionType<CustomFunction> {

	@Override
	public String getHandlerChain(CustomFunction function) {
		return function.getHandlerChain().get();
	}

	@Override
	public Map<String, String> getHandlerProperties(CustomFunction function) {
		return null;
	}

	@Override
	public CustomFunction newFunction() {
		return new CustomFunction();
	}
}
