package step.core.execution.type;

import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngineContext;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;

@Plugin
public class ExecutionTypePlugin extends AbstractExecutionEnginePlugin {

    private ExecutionTypeManager executionTypeManager;

    @Override
    public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext executionEngineContext) {
        super.initializeExecutionEngineContext(parentContext, executionEngineContext);
        executionTypeManager = executionEngineContext.inheritFromParentOrComputeIfAbsent(parentContext, ExecutionTypeManager.class, k -> new ExecutionTypeManager());
    }

    @Override
    public void executionStart(ExecutionContext context) {
        context.put(ExecutionTypeManager.class, executionTypeManager);
    }
}
