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

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import step.core.access.AuthenticationManager;
import step.core.access.Preferences;
import step.core.access.User;
import step.core.access.UserAccessor;
import step.core.controller.ControllerSetting;
import step.core.controller.ControllerSettingAccessor;

@Singleton
@Path("admin")
public class AdminServices extends AbstractServices {
	
	protected ControllerSettingAccessor controllerSettingsAccessor;

	private static final String MAINTENANCE_MESSAGE_KEY = "maintenance_message";
	private static final String MAINTENANCE_TOGGLE_KEY = "maintenance_message_enabled";
	
	private AuthenticationManager authenticationManager;

	@PostConstruct
	public void init() throws Exception {
		super.init();
		controllerSettingsAccessor = getContext().require(ControllerSettingAccessor.class);
		authenticationManager = getContext().require(AuthenticationManager.class);
	}

	@POST
	@Secured(right="user-write")
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/user")
	public void save(User user) {
		UserAccessor userAccessor = getContext().getUserAccessor();

		User previousUser = userAccessor.get(user.getId());
		if (previousUser == null) {
			// previousUser is null => we're creating a new user
			// initializing password, is now down in reset password
		}
		userAccessor.save(user);
	}

	@DELETE
	@Secured(right="user-write")
	@Path("/user/{id}")
	public void remove(@PathParam("id") String username) {
		getContext().getUserAccessor().remove(username);
	}
	
	@GET
	@Secured(right="user-read")
	@Path("/user/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public User getUser(@PathParam("id") String username) {
		return getContext().getUserAccessor().getByUsername(username);
	}
	
	@GET
	@Secured(right="user-read")
	@Path("/users")
	@Produces(MediaType.APPLICATION_JSON)
	public List<User> getUserList() {
		return getContext().getUserAccessor().getAllUsers();
	}
	
	public static class ChangePwdRequest {
		
		private String oldPwd;
		
		private String newPwd;

		public ChangePwdRequest() {
			super();
		}

		public String getOldPwd() {
			return oldPwd;
		}

		public void setOldPwd(String oldPwd) {
			this.oldPwd = oldPwd;
		}

		public String getNewPwd() {
			return newPwd;
		}

		public void setNewPwd(String newPwd) {
			this.newPwd = newPwd;
		}
	}
	
	@GET
	@Path("/maintenance/message")
	public String getMaintenanceMessage() {
		ControllerSetting setting = controllerSettingsAccessor.getSettingByKey(MAINTENANCE_MESSAGE_KEY);
		return setting!=null?setting.getValue():null;
	}
	
	@POST
	@Secured(right="admin")
	@Path("/maintenance/message")
	public void setMaintenanceMessage(String message) {
		ControllerSetting setting = controllerSettingsAccessor.getSettingByKey(MAINTENANCE_MESSAGE_KEY);
		if(setting == null) {
			setting = new ControllerSetting();
			setting.setKey(MAINTENANCE_MESSAGE_KEY);
		}
		setting.setValue(message);
		controllerSettingsAccessor.save(setting);
	}
	
	@GET
	@Path("/maintenance/message/toggle")
	public boolean getMaintenanceMessageToggle() {
		ControllerSetting setting = controllerSettingsAccessor.getSettingByKey(MAINTENANCE_TOGGLE_KEY);
		return setting!=null?Boolean.parseBoolean(setting.getValue()):false;
	}
	
	@POST
	@Secured(right="admin")
	@Path("/maintenance/message/toggle")
	public void setMaintenanceMessageToggle(boolean enabled) {
		ControllerSetting setting = controllerSettingsAccessor.getSettingByKey(MAINTENANCE_TOGGLE_KEY);
		if(setting == null) {
			setting = new ControllerSetting();
			setting.setKey(MAINTENANCE_TOGGLE_KEY);
		}
		setting.setValue(Boolean.toString(enabled));
		controllerSettingsAccessor.save(setting);
	}
	
	@POST
	@Secured
	@Path("/myaccount/changepwd")
	public void resetMyPassword(ChangePwdRequest request) {
		User user = getCurrentUser();
		if(user!=null) {
			user.setPassword(authenticationManager.encryptPwd(request.getNewPwd()));
			user.addCustomField("otp", false);
			getContext().getUserAccessor().save(user);
			getSession().setUser(user);
		}
	}

	protected User getCurrentUser() {
		return getContext().getUserAccessor().get(getSession().getUser().getId());
	}
	
	@GET
	@Secured
	@Path("/myaccount")
	@Produces(MediaType.APPLICATION_JSON)
	public User getMyUser() {
		User user = getCurrentUser();
		return user;
	}
		
	@GET
	@Secured
	@Path("/myaccount/preferences")
	public Preferences getPreferences() {
		User user = getCurrentUser();
		if(user!=null) {
			return user.getPreferences();
		} else {
			return null;
		}
	}
	
	@POST
	@Secured
	@Path("/myaccount/preferences/{id}")
	public void putPreference(@PathParam("id") String preferenceName, Object value) {
		User user = getCurrentUser();
		if(user!=null) {
			if(user.getPreferences()==null) {
				Preferences prefs = new Preferences();
				user.setPreferences(prefs);
			}
			user.getPreferences().put(preferenceName, value);
			getContext().getUserAccessor().save(user);			
		}
	}
	
	@POST
	@Secured
	@Path("/myaccount/preferences")
	public void putPreference( Preferences preferences) {
		User user = getCurrentUser();
		if(user!=null) {
			user.setPreferences(preferences);
			getContext().getUserAccessor().save(user);			
		}
	}
	
	@POST
	@Secured(right="user-write")
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/user/{id}/resetpwd")
	public Password resetPassword(@PathParam("id") String username) {
		User user = getContext().getUserAccessor().getByUsername(username);
		String pwd = authenticationManager.resetPwd(user);
		getContext().getUserAccessor().save(user);
		Password password = new Password();
		password.setPassword(pwd);
		return password;
	}
	
	public static class Password {
		String password;
		public String getPassword() {
			return password;
		}
		public void setPassword(String password) {
			this.password = password;
		}
	}


}