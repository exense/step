package step.functions.type;

import step.functions.Function;

public interface FunctionTypeRegistry {

	public AbstractFunctionType<Function> getFunctionType(String functionType);
	
	public AbstractFunctionType<Function> getFunctionTypeByFunction(Function function);

	void registerFunctionType(AbstractFunctionType<? extends Function> functionType);
}
