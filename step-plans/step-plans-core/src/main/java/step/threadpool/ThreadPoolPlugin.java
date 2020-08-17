package step.threadpool;

import step.core.execution.ExecutionContext;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;

@Plugin
public class ThreadPoolPlugin extends AbstractExecutionEnginePlugin {

	@Override
	public void executionStart(ExecutionContext executionContext) {
		executionContext.put(ThreadPool.class, new ThreadPool(executionContext));
	}

}
