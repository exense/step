package step.plugins.datatable;

import step.core.GlobalContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;

@Plugin
public class DataTablePlugin extends AbstractPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) {
		context.getServiceRegistrationCallback().registerService(DataTableServices.class);
	}

}
