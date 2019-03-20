package step.core.execution.type;

import step.core.GlobalContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;

@Plugin
public class ExecutionTypePlugin extends AbstractPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		super.executionControllerStart(context);
		
		ExecutionTypeManager typeManager = new ExecutionTypeManager();
		context.put(ExecutionTypeManager.class, typeManager);
	}
}
