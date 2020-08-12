package step.engine.plugins;

import step.core.execution.ExecutionEngineContext;
import step.core.plugins.ExecutionCallbacks;

public interface ExecutionEnginePlugin extends ExecutionCallbacks {

	public void initialize(ExecutionEngineContext context);
}
