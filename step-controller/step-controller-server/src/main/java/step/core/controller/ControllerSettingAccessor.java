package step.core.controller;

import javax.json.JsonObjectBuilder;

import step.core.accessors.AbstractCRUDAccessor;
import step.core.accessors.MongoClientSession;
import step.core.json.JsonProviderCache;

public class ControllerSettingAccessor extends AbstractCRUDAccessor<ControllerSetting> {

	public ControllerSettingAccessor(MongoClientSession clientSession) {
		super(clientSession, "settings", ControllerSetting.class);
	}
	
	public ControllerSetting getSettingByKey(String key) {
		JsonObjectBuilder builder = JsonProviderCache.createObjectBuilder();
		builder.add("key", key);
		String query = builder.build().toString();
		return collection.findOne(query).as(ControllerSetting.class);
	}
}
