package step.plugins.functions.types;

import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionEngineContext;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.BasePlugin;
import step.functions.type.FunctionTypeRegistry;

@Plugin(dependencies= {BasePlugin.class})
public class CompositeFunctionTypePlugin extends AbstractExecutionEnginePlugin {

	@Override
	public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
		FunctionTypeRegistry functionTypeRegistry = context.require(FunctionTypeRegistry.class);		
		functionTypeRegistry.registerFunctionType(new CompositeFunctionType(context.getPlanAccessor()));
	}
}
