package step.plugins.measurements.log;

import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.plugins.measurements.MeasurementPlugin;

@Plugin
public class LogMeasurementControllerPlugin extends AbstractControllerPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		MeasurementPlugin.registerMeasurementHandlers(new LogMeasurementHandler());
	}

	@Override
	public void executionControllerDestroy(GlobalContext context) {

	}

}
