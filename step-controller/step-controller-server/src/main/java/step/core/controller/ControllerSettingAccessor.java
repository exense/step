package step.core.controller;

public interface ControllerSettingAccessor {

	ControllerSetting getSettingByKey(String key);
	
	ControllerSetting updateOrCreateSetting(String key, String value);

	boolean getSettingAsBoolean(String settingSchedulerEnabled);

	ControllerSetting save(ControllerSetting controllerSetting);

	ControllerSetting createSettingIfNotExisting(String settingSchedulerEnabled, String string);
}
