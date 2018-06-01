package step.functions;

import step.functions.type.AbstractFunctionType;

public interface FunctionTypeRegistry {

	public AbstractFunctionType<Function> getFunctionTypeByFunction(Function function);

	void registerFunctionType(AbstractFunctionType<? extends Function> functionType);
}
