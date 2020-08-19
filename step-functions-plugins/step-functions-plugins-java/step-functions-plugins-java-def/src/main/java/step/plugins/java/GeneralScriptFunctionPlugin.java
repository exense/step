package step.plugins.java;

import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionEngineContext;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;
import step.engine.plugins.FunctionPlugin;
import step.functions.type.FunctionTypeRegistry;

@Plugin(dependencies= {FunctionPlugin.class})
public class GeneralScriptFunctionPlugin extends AbstractExecutionEnginePlugin {
	
	@Override
	public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext, ExecutionEngineContext context) {
		FunctionTypeRegistry functionTypeRegistry = context.require(FunctionTypeRegistry.class);
		functionTypeRegistry.registerFunctionType(new GeneralScriptFunctionType(context.getConfiguration()));
	}

}
