package step.functions.base.types;

import java.util.HashMap;
import java.util.Map;

import step.functions.base.types.handler.LocalFunctionHandler;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeException;
import step.functions.type.SetupFunctionException;

public class LocalFunctionType extends AbstractFunctionType<LocalFunction> {

	@Override
	public void init() {
		super.init();
	}

	@Override
	public String getHandlerChain(LocalFunction function) {
		return LocalFunctionHandler.class.getName();
	}

	@Override
	public Map<String, String> getHandlerProperties(LocalFunction function) {
		Map<String, String> props = new HashMap<>();
		return props;
	}

	@Override
	public void setupFunction(LocalFunction function) throws SetupFunctionException {

	}

	@Override
	public LocalFunction copyFunction(LocalFunction function) throws FunctionTypeException {
		LocalFunction copy = super.copyFunction(function);
		return copy;
	}

	@Override
	public LocalFunction newFunction() {
		return new LocalFunction();
	}

}
