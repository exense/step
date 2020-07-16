package step.plugins.java;

import step.core.execution.ExecutionEngineContext;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.functions.type.FunctionTypeRegistry;

@Plugin(dependencies= {})
public class JavaPlugin extends AbstractExecutionEnginePlugin {
	
	@Override
	public void initialize(ExecutionEngineContext context) {
		FunctionTypeRegistry functionTypeRegistry = context.get(FunctionTypeRegistry.class);
		functionTypeRegistry.registerFunctionType(new GeneralScriptFunctionType(context.getConfiguration()));
	}

}
