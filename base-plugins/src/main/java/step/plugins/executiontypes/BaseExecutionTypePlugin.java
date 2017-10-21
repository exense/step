package step.plugins.executiontypes;

import step.core.GlobalContext;
import step.core.execution.type.ExecutionTypeManager;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;

@Plugin(prio=100)
public class BaseExecutionTypePlugin extends AbstractPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		super.executionControllerStart(context);
		
		ExecutionTypeManager executionTypeManager = context.get(ExecutionTypeManager.class);
		executionTypeManager.put(new DefaultExecutionType(context));
		executionTypeManager.put(new TestSetExecutionType(context));

	}

}
