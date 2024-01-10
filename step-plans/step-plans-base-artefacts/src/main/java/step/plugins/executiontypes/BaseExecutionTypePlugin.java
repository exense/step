package step.plugins.executiontypes;

import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionEngineContext;
import step.core.execution.type.ExecutionTypeManager;
import step.core.execution.type.ExecutionTypePlugin;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;

@Plugin(dependencies = ExecutionTypePlugin.class)
public class BaseExecutionTypePlugin extends AbstractExecutionEnginePlugin {

    @Override
    public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext executionEngineContext) {
        super.initializeExecutionEngineContext(parentContext, executionEngineContext);
        ExecutionTypeManager executionTypeManager = executionEngineContext.require(ExecutionTypeManager.class);
        if (executionTypeManager.get(DefaultExecutionType.NAME) == null) {
            executionTypeManager.put(new DefaultExecutionType(executionEngineContext));
        }
        if (executionTypeManager.get(TestSetExecutionType.NAME) == null) {
            executionTypeManager.put(new TestSetExecutionType(executionEngineContext));
        }
    }
}
