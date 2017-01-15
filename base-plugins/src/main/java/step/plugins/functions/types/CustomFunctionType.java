package step.plugins.functions.types;

import java.util.Map;

import step.functions.Function;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionType;

@FunctionType(name="custom",label="Custom")
public class CustomFunctionType extends AbstractFunctionType<CustomFunctionTypeConf> {

	@Override
	public String getHandlerChain(Function function) {
		return getFunctionConf(function).getHanlderChain();
	}

	@Override
	public Map<String, String> getHandlerProperties(Function function) {
		return null;
	}

	@Override
	public CustomFunctionTypeConf newFunctionTypeConf() {
		return new CustomFunctionTypeConf();
	}

}
