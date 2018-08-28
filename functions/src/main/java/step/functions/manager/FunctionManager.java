package step.functions.manager;

import java.util.Map;

import step.functions.Function;
import step.functions.type.FunctionTypeException;
import step.functions.type.SetupFunctionException;

public interface FunctionManager {

	Function saveFunction(Function function) throws SetupFunctionException, FunctionTypeException;

	Function copyFunction(String functionId) throws FunctionTypeException;

	void deleteFunction(String functionId) throws FunctionTypeException;

	Function newFunction(String functionType);
	
	Function getFunctionByAttributes(Map<String, String> attributes);
	
	Function getFunctionById(String id);

}