package step.core.controller;

import step.core.accessors.Accessor;

import java.util.List;

public interface ControllerSettingAccessor extends Accessor<ControllerSetting> {

	ControllerSetting getSettingByKey(String key);
	
	ControllerSetting updateOrCreateSetting(String key, String value);

	boolean getSettingAsBoolean(String settingSchedulerEnabled);

	ControllerSetting save(ControllerSetting controllerSetting);

	ControllerSetting createSettingIfNotExisting(String settingSchedulerEnabled, String string);

	void addHook(String key, ControllerSettingHook hook);

	boolean removeHook(String key, ControllerSettingHook hook);

}
