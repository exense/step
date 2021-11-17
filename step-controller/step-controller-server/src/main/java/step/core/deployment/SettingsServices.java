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
package step.core.deployment;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.tags.Tag;
import step.core.controller.ControllerSetting;
import step.core.controller.ControllerSettingAccessor;

@Singleton
@Path("settings")
@Tag(name = "Settings")
public class SettingsServices extends AbstractServices {
	
	protected ControllerSettingAccessor controllerSettingsAccessor;

	@PostConstruct
	public void init() throws Exception {
		super.init();
		controllerSettingsAccessor = getContext().require(ControllerSettingAccessor.class);
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/{id}")
	@Secured(right="admin")
	public void save(@PathParam("id") String key, String value) {
		ControllerSetting setting = controllerSettingsAccessor.getSettingByKey(key);
		if(setting == null) {
			setting = new ControllerSetting();
			setting.setKey(key);
		}
		setting.setValue(value);
		controllerSettingsAccessor.save(setting);
	}

	@DELETE
	@Path("/{id}")
	@Secured(right="admin")
	public void delete(@PathParam("id") String key) {
		ControllerSetting setting = controllerSettingsAccessor.getSettingByKey(key);
		if(setting != null) {
			controllerSettingsAccessor.remove(setting.getId());
		}
	}
	
	@GET
	@Path("/{id}")
	@Produces(MediaType.TEXT_PLAIN)
	@Secured(right="settings-read")
	public String get(@PathParam("id") String key) {
		ControllerSetting setting = controllerSettingsAccessor.getSettingByKey(key);
		if(setting != null) {
			return setting.getValue();
		} else {
			return null;
		}
	}
}
