package step.engine.plugins;

import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.plugins.ExecutionCallbacks;

public interface ExecutionEnginePlugin extends ExecutionCallbacks {

	public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext executionEngineContext);
	
	public void initializeExecutionContext(ExecutionEngineContext executionEngineContext, ExecutionContext executionContext);
}
