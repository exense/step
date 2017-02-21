package step.plugins.functions.types;

import java.util.Map;

import step.functions.Function;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionType;

@FunctionType(name="custom",label="Custom Handler")
public class CustomFunctionType extends AbstractFunctionType {

	@Override
	public String getHandlerChain(Function function) {
		return function.getConfiguration().getString("handlerChain");
	}

	@Override
	public Map<String, String> getHandlerProperties(Function function) {
		return null;
	}
}
