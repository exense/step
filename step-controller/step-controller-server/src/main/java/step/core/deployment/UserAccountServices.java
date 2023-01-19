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

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import step.core.access.Preferences;
import step.core.access.User;
import step.framework.server.security.Secured;

@Singleton
@Path("admin")
@Tag(name = "User")
public class UserAccountServices extends AbstractStepServices {


	@PostConstruct
	public void init() throws Exception {
		super.init();
	}

	protected User getCurrentUser() {
		return getContext().getUserAccessor().get(getSession().getUser().getId());
	}
	
	@GET
	@Secured
	@Path("/myaccount")
	@Produces(MediaType.APPLICATION_JSON)
	public User getMyUser() {
		return getCurrentUser();
	}
		
	@GET
	@Secured
	@Produces(MediaType.APPLICATION_JSON)
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
	@Consumes(MediaType.APPLICATION_JSON)
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
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/myaccount/preferences")
	public void putPreferences( Preferences preferences) {
		User user = getCurrentUser();
		if(user!=null) {
			user.setPreferences(preferences);
			getContext().getUserAccessor().save(user);			
		}
	}
	



}
