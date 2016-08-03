package step.grid.agent.handler;

import java.util.concurrent.Callable;

public abstract class MessageHandlerDelegate implements MessageHandler {

	protected MessageHandler delegate;
	
	public MessageHandler getDelegate() {
		return delegate;
	}

	public void setDelegate(MessageHandler delegate) {
		this.delegate = delegate;
	}

	<T extends Object> T runInContext(Callable<T> runnable) throws Exception {
		return runnable.call();	
	}
}
