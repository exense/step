package step.functions.execution;

import java.util.Map;

import step.functions.Input;
import step.functions.Output;
import step.grid.TokenWrapper;
import step.grid.tokenpool.Interest;

public interface FunctionExecutionService {

	TokenWrapper getLocalTokenHandle();

	TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests, boolean createSession) throws FunctionExecutionServiceException;
	
	void returnTokenHandle(TokenWrapper adapterToken) throws FunctionExecutionServiceException;

	 <IN,OUT> Output<OUT> callFunction(TokenWrapper tokenHandle, Map<String, String> functionAttributes, Input<IN> input, Class<OUT> outputClass);

	 <IN,OUT> Output<OUT> callFunction(TokenWrapper tokenHandle, String functionId, Input<IN> input, Class<OUT> outputClass);

}