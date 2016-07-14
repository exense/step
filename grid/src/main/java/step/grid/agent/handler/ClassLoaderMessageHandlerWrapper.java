package step.grid.agent.handler;

import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class ClassLoaderMessageHandlerWrapper implements MessageHandler, MessageHandlerDelegate {

	ClassLoader cl;
	
	MessageHandler delegate;

	public ClassLoaderMessageHandlerWrapper(ClassLoader cl) {
		super();
		this.cl = cl;
	}

	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {

		ClassLoader previousCl = Thread.currentThread().getContextClassLoader();
		
		Thread.currentThread().setContextClassLoader(cl);
		try {
			return delegate.handle(token, message);
		} finally {
			Thread.currentThread().setContextClassLoader(previousCl);
		}
	}

	@Override
	public void setDelegate(MessageHandler delegate) {
		this.delegate = delegate;
	} 
}
