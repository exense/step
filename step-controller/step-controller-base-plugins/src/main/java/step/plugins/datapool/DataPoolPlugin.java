package step.plugins.datapool;

import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.datapool.excel.ExcelFunctions;

@Plugin
public class DataPoolPlugin extends AbstractControllerPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		context.getServiceRegistrationCallback().registerService(DataPoolPluginServices.class);
		
		// forced to set the configuration using the static method as long as ExcelFunction is used in a static way
		ExcelFunctions.setConfiguration(context.getConfiguration());
	}

}
