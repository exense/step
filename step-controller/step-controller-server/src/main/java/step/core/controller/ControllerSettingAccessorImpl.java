/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.core.controller;

import javax.json.JsonObjectBuilder;

import step.core.accessors.AbstractCRUDAccessor;
import step.core.accessors.MongoClientSession;
import step.core.json.JsonProviderCache;

public class ControllerSettingAccessorImpl extends AbstractCRUDAccessor<ControllerSetting> implements ControllerSettingAccessor {
	
	public ControllerSettingAccessorImpl(MongoClientSession clientSession) {
		super(clientSession, "settings", ControllerSetting.class);
	}
	
	public ControllerSetting getSettingByKey(String key) {
		JsonObjectBuilder builder = JsonProviderCache.createObjectBuilder();
		builder.add("key", key);
		String query = builder.build().toString();
		return collection.findOne(query).as(ControllerSetting.class);
	}
	
	// TODO: the following methods should be moved to a ControllerSettingManager.
	// They actually don't belong to an accessor which role should be limited to
	// retrieval and persistence of data
	public ControllerSetting updateOrCreateSetting(String key, String value) {
		ControllerSetting setting = getOrCreateSettingByKey(key);
		setting.setValue(value);
		return save(setting);
	}

	public ControllerSetting createSettingIfNotExisting(String key, String value) {
		ControllerSetting schedulerEnabled = getSettingByKey(key);
		if (schedulerEnabled == null) {
			return save(new ControllerSetting(key, value));
		} else {
			return schedulerEnabled;
		}
	}
	
	public boolean getSettingAsBoolean(String key) {
		ControllerSetting setting = getSettingByKey(key);
		return setting != null ? Boolean.valueOf(setting.getValue()) : false;
	}
	
	private ControllerSetting getOrCreateSettingByKey(String key) {
		ControllerSetting setting = getSettingByKey(key);
		if(setting == null) {
			setting = new ControllerSetting();
			setting.setKey(key);
		}
		return setting;
	}
	// End of TODO
}
