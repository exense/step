package step.grid.agent.handler;

import java.lang.reflect.Method;

import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class MethodDispatcherHandler extends MessageHandlerDelegate {
	
	public MethodDispatcherHandler() {
		super();
	}

	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		for(Method m:delegate.getClass().getMethods()) {
			if(m.getName().equals(message.getFunction())) {
				return (OutputMessage) m.invoke(delegate, token, message);
			}
		}
		throw new Exception("Unable to find method '"+message.getFunction()+"' in class "+delegate.getClass());
	}
}
