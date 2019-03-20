package step.core.controller;

import javax.json.JsonObjectBuilder;
import javax.json.spi.JsonProvider;

import step.core.accessors.AbstractCRUDAccessor;
import step.core.accessors.MongoClientSession;

public class ControllerSettingAccessor extends AbstractCRUDAccessor<ControllerSetting> {

	private static JsonProvider jsonProvider = JsonProvider.provider();
	
	public ControllerSettingAccessor(MongoClientSession clientSession) {
		super(clientSession, "settings", ControllerSetting.class);
	}
	
	public ControllerSetting getSettingByKey(String key) {
		JsonObjectBuilder builder = jsonProvider.createObjectBuilder();
		builder.add("key", key);
		String query = builder.build().toString();
		return collection.findOne(query).as(ControllerSetting.class);
	}
}
