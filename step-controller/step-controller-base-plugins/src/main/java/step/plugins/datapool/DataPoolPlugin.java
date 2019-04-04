package step.plugins.datapool;

import step.core.GlobalContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.datapool.excel.ExcelFunctions;

@Plugin
public class DataPoolPlugin extends AbstractPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		context.getServiceRegistrationCallback().registerService(DataPoolPluginServices.class);
		
		// forced to set the max excel size using the static method as long as ExcelFunction is used in a static way
		Integer maxExcelSize = context.getConfiguration().getPropertyAsInteger("tec.maxexcelsize");
		ExcelFunctions.setMaxExcelSize(maxExcelSize);
	}

}
