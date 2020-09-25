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
