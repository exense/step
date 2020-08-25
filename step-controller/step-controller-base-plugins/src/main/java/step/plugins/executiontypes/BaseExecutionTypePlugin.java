package step.plugins.executiontypes;

import step.core.GlobalContext;
import step.core.execution.type.ExecutionTypeManager;
import step.core.execution.type.ExecutionTypePlugin;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.plugins.views.ViewControllerPlugin;

@Plugin(dependencies= {ExecutionTypePlugin.class, ViewControllerPlugin.class})
public class BaseExecutionTypePlugin extends AbstractControllerPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		super.executionControllerStart(context);
		
		ExecutionTypeManager executionTypeManager = context.get(ExecutionTypeManager.class);
		executionTypeManager.put(new DefaultExecutionType(context));
		executionTypeManager.put(new TestSetExecutionType(context));

	}

}
