package step.functions;

import java.util.Map;

import step.grid.TokenWrapper;
import step.grid.client.GridClient.AgentCommunicationException;
import step.grid.tokenpool.Interest;

public interface FunctionExecutionService {

	TokenWrapper getLocalTokenHandle();

	TokenWrapper getTokenHandle(Map<String, String> attributes, Map<String, Interest> interests, boolean createSession) throws AgentCommunicationException;
	
	void returnTokenHandle(TokenWrapper adapterToken) throws AgentCommunicationException;

	Output callFunction(TokenWrapper tokenHandle, Map<String, String> functionAttributes, Input input);

	Output callFunction(TokenWrapper tokenHandle, String functionId, Input input);

}