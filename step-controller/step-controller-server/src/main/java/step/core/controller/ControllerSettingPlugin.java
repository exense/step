package step.core.controller;

import step.core.GlobalContext;
import step.core.execution.ExecutionContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.variables.VariableType;

@Plugin
public class ControllerSettingPlugin extends AbstractControllerPlugin {

	private ControllerSettingAccessor controllerSettingAccessor;

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		controllerSettingAccessor = new ControllerSettingAccessor(context.getMongoClientSession());
	}

	@Override
	public void executionStart(ExecutionContext context) {
		context.getVariablesManager().putVariable(context.getCurrentReportNode(), VariableType.IMMUTABLE, "controllerSettings", 
				new ControllerSettingsService(controllerSettingAccessor));
	}
	
	public static class ControllerSettingsService {
		
		private ControllerSettingAccessor controllerSettingAccessor;

		public ControllerSettingsService(ControllerSettingAccessor controllerSettingAccessor) {
			super();
			this.controllerSettingAccessor = controllerSettingAccessor;
		}

		public ControllerSetting getSettingByKey(String key) {
			return controllerSettingAccessor.getSettingByKey(key);
		}
	}

}
