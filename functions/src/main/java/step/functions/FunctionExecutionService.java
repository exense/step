package step.functions;

import java.util.Map;

import step.grid.TokenWrapper;
import step.grid.tokenpool.Interest;

public interface FunctionExecutionService {

	TokenWrapper getLocalTokenHandle();

	TokenWrapper getTokenHandle();

	TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests);

	void returnTokenHandle(TokenWrapper adapterToken);

	Output callFunction(TokenWrapper tokenHandle, Map<String, String> functionAttributes, Input input);

	Output callFunction(TokenWrapper tokenHandle, String functionId, Input input);

}