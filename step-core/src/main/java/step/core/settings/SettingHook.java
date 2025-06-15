package step.core.settings;

import org.bson.types.ObjectId;

public interface SettingHook<T> {
	void onSettingSave(T setting);
	void onSettingRemove(ObjectId settingId, T removed);
}
