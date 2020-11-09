package step.core.controller;

import java.util.ArrayList;
import java.util.List;

import step.core.accessors.InMemoryCRUDAccessor;

public class InMemoryControllerSettingAccessor extends InMemoryCRUDAccessor<ControllerSetting> implements ControllerSettingAccessor {

	private List<ControllerSetting> controllerSettings = new ArrayList<ControllerSetting>();
	
	@Override
	public ControllerSetting getSettingByKey(String key) { 
		return controllerSettings.stream()
				.filter(s -> s.getKey().equals(key))
				.findFirst()
				.orElse(null);
	}

	@Override
	public ControllerSetting updateOrCreateSetting(String key, String value) {
		ControllerSetting setting = getSettingByKey(key);
		if(setting == null) {
			setting = new ControllerSetting();
			setting.setKey(key);
		} 
		setting.setValue(value);
		controllerSettings.add(setting);
		return setting;
	}


	@Override
	public boolean getSettingAsBoolean(String key) {
		ControllerSetting controllerSetting = getSettingByKey(key);
		return Boolean.valueOf(controllerSetting.getValue());		
	}

	@Override
	public ControllerSetting createSettingIfNotExisting(String key, String value) {
		ControllerSetting setting = getSettingByKey(key);
		if (setting == null) {
			return save(new ControllerSetting(key, value));
		} else {
			return setting;
		}
	}
}
