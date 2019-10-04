package step.functions.execution;

import java.util.Map;

import step.functions.Function;
import step.functions.io.FunctionInput;
import step.functions.io.Output;
import step.grid.TokenWrapper;
import step.grid.TokenWrapperOwner;
import step.grid.tokenpool.Interest;

public interface FunctionExecutionService {

	TokenWrapper getLocalTokenHandle();

	TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests, boolean createSession, TokenWrapperOwner tokenWrapperOwner) throws FunctionExecutionServiceException;
	
	void returnTokenHandle(String tokenHandleId) throws FunctionExecutionServiceException;

	<IN,OUT> Output<OUT> callFunction(String tokenHandleId, Function function, FunctionInput<IN> functionInput, Class<OUT> outputClass);

}