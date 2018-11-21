package step.functions.handler;

import step.grid.agent.tokenpool.AgentTokenWrapper;

public class FunctionHandlerFactory {

	public AbstractFunctionHandler create(ClassLoader classloader, String class_, AgentTokenWrapper agentTokenWrapper) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		AbstractFunctionHandler functionHandler = (AbstractFunctionHandler) classloader.loadClass(class_).newInstance();
		initialize(agentTokenWrapper, functionHandler);
		return functionHandler;
	}
	
	public AbstractFunctionHandler initialize(AgentTokenWrapper agentTokenWrapper, AbstractFunctionHandler handler) {
		handler.initialize(agentTokenWrapper);
		return handler;
	}
}
