package step.plugins.functions.types;

import java.util.Map;

import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionType;

@FunctionType(name="custom",label="Custom")
public class CustomFunctionType extends AbstractFunctionType<CustomFunctionTypeConf> {

	@Override
	public String getHandlerChain(CustomFunctionTypeConf functionTypeConf) {
		return functionTypeConf.getHanlderChain();
	}

	@Override
	public Map<String, String> getHandlerProperties(CustomFunctionTypeConf functionTypeConf) {
		return null;
	}

	@Override
	public CustomFunctionTypeConf newFunctionTypeConf() {
		return new CustomFunctionTypeConf();
	}

}
