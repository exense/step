package step.functions.handler;

import step.grid.agent.tokenpool.AgentTokenWrapper;

public class FunctionHandlerFactory {

	public AbstractFunctionHandler create(AgentTokenWrapper agentTokenWrapper, String class_) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		AbstractFunctionHandler functionHandler = (AbstractFunctionHandler) Thread.currentThread().getContextClassLoader().loadClass(class_).newInstance();
		initialize(agentTokenWrapper, functionHandler);
		return functionHandler;
	}
	
	public AbstractFunctionHandler initialize(AgentTokenWrapper agentTokenWrapper, AbstractFunctionHandler handler) {
		handler.initialize(agentTokenWrapper);
		return handler;
	}
}
