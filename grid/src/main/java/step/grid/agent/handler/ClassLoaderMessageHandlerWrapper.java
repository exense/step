package step.grid.agent.handler;

import java.util.concurrent.Callable;

import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class ClassLoaderMessageHandlerWrapper extends MessageHandlerDelegate {

	ClassLoader cl;
	
	public ClassLoaderMessageHandlerWrapper(ClassLoader cl) {
		super();
		this.cl = cl;
	}

	@Override
	public OutputMessage handle(final AgentTokenWrapper token, final InputMessage message) throws Exception {
		return runInContext(new Callable<OutputMessage>() {
			@Override
			public OutputMessage call() throws Exception {
				return delegate.handle(token, message);
			}
		});
	}

	@Override
	public <T> T runInContext(Callable<T> runnable) throws Exception {
		ClassLoader previousCl = Thread.currentThread().getContextClassLoader();
		
		Thread.currentThread().setContextClassLoader(cl);
		try {
			return runnable.call();
		} finally {
			Thread.currentThread().setContextClassLoader(previousCl);
		}
	} 
}
