/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.plugins.screentemplating;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.bson.types.ObjectId;

import step.core.deployment.AbstractServices;
import step.core.deployment.Secured;
import step.core.deployment.Session;

@Singleton
@Path("screens")
public class ScreenTemplateService extends AbstractServices {
	
	protected ScreenTemplateManager screenTemplateManager;
	protected ScreenInputAccessor screenInputAccessor;
	
	@PostConstruct
	public void init() {
		screenInputAccessor = getContext().get(ScreenInputAccessor.class);
		screenTemplateManager = getContext().get(ScreenTemplateManager.class);
	}
	
	@GET
	@Secured
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Input> getInputsForScreen(@PathParam("id") String screenId, @Context UriInfo uriInfo, @Context ContainerRequestContext crc) {		
		Map<String, Object> contextBindings = getContextBindings(uriInfo, crc);
		return screenTemplateManager.getInputsForScreen(screenId, contextBindings);
	}
	
	@GET
	@Secured
	@Path("/input/byscreen/{screenid}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<ScreenInput> getScreenInputsByScreenId(@PathParam("screenid") String screenId) {		
		return screenInputAccessor.getScreenInputsByScreenId(screenId);
	}
	
	@GET
	@Secured
	@Path("/input/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public ScreenInput getInput(@PathParam("id") String id) {
		return screenInputAccessor.get(new ObjectId(id));
	}
	
	@DELETE
	@Secured(right="admin")
	@Path("/input/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public void deleteInput(@PathParam("id") String id) {		
		screenInputAccessor.remove(new ObjectId(id));
		screenTemplateManager.notifyChange();
	}
	
	@POST
	@Secured(right="admin")
	@Path("/input")
	@Produces(MediaType.APPLICATION_JSON)
	public void saveInput(ScreenInput screenInput) {
		screenInputAccessor.save(screenInput);
		screenTemplateManager.notifyChange();
	}

	private Map<String, Object> getContextBindings(UriInfo uriInfo, ContainerRequestContext crc) {
		Map<String, Object> contextBindings = new HashMap<>();
		
		Session session = (Session) crc.getProperty("session");
		if(session!=null) {
			contextBindings.put("user", session.getUsername());
			if(session.getProfile()!= null)
				contextBindings.put("role", session.getProfile().getRole());
		}
		
		for(String key:uriInfo.getQueryParameters().keySet()) {
			contextBindings.put(key, uriInfo.getQueryParameters().getFirst(key));
		}
		return contextBindings;
	}

}
