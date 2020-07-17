package step.plugins.functions.types;

import step.core.execution.ExecutionEngineContext;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.BasePlugin;
import step.functions.type.FunctionTypeRegistry;

@Plugin(dependencies= {BasePlugin.class})
public class CompositeFunctionTypePlugin extends AbstractExecutionEnginePlugin {

	@Override
	public void initialize(ExecutionEngineContext context) {
		FunctionTypeRegistry functionTypeRegistry = context.get(FunctionTypeRegistry.class);		
		functionTypeRegistry.registerFunctionType(new CompositeFunctionType(context.getPlanAccessor()));
	}
}
