package step.core.controller;

import org.bson.types.ObjectId;

public interface ControllerSettingHook {
	void onSettingSave(ControllerSetting setting);
	void onSettingRemove(ObjectId setting);
}
