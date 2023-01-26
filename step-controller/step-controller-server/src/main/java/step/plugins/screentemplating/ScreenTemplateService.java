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
package step.plugins.screentemplating;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.bson.types.ObjectId;

import step.core.GlobalContext;
import step.framework.server.access.AuthorizationManager;
import step.core.access.Role;
import step.core.access.User;
import step.core.accessors.AbstractOrganizableObject;
import step.core.deployment.AbstractStepServices;
import step.framework.server.security.Secured;
import step.framework.server.Session;
import step.core.objectenricher.ObjectPredicate;
import step.core.objectenricher.ObjectPredicateFactory;

@Singleton
@Path("screens")
@Tag(name = "Screens")
public class ScreenTemplateService extends AbstractStepServices {
	
	protected AuthorizationManager authorizationManager;
	protected ScreenTemplateManager screenTemplateManager;
	protected ScreenInputAccessor screenInputAccessor;
	protected ObjectPredicateFactory objectPredicateFactory;
	
	@PostConstruct
	public void init() throws Exception {
		super.init();
		GlobalContext context = getContext();
		authorizationManager = context.get(AuthorizationManager.class);
		screenInputAccessor = context.get(ScreenInputAccessor.class);
		screenTemplateManager = context.get(ScreenTemplateManager.class);
		objectPredicateFactory = context.get(ObjectPredicateFactory.class);
	}
	
	@GET
	@Secured
	@Produces(MediaType.APPLICATION_JSON)
	public Set<String> getScreens() {		
		HashSet<String> screens = new HashSet<>();
		screenInputAccessor.getAll().forEachRemaining(s->{
			String screenId = s.getScreenId();
			if(screenId!=null) {
				screens.add(screenId);
			}
		});
		return screens;
	}
	
	@GET
	@Secured
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Input> getInputsForScreenGet(@PathParam("id") String screenId, @Context UriInfo uriInfo) {
		Map<String, Object> contextBindings = getContextBindings(uriInfo);
		ObjectPredicate objectPredicate = objectPredicateFactory.getObjectPredicate(getSession());
		return screenTemplateManager.getInputsForScreen(screenId, contextBindings, objectPredicate);
	}
	
	@SuppressWarnings("unchecked")
	@POST
	@Secured
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public List<Input> getInputsForScreenPost(@PathParam("id") String screenId, Object params) {
		ObjectPredicate objectPredicate = objectPredicateFactory.getObjectPredicate(getSession());
		Map<String, Object> contextBindings = getContextBindings(null);
		if(params != null && params instanceof Map) {
			contextBindings.putAll((Map<String, Object>) params);
		}
		return screenTemplateManager.getInputsForScreen(screenId, contextBindings, objectPredicate);
	}
	
	@GET
	@Secured
	@Path("/{screenid}/{inputid}")
	@Produces(MediaType.APPLICATION_JSON)
	public Input getInputForScreen(@PathParam("screenid") String screenId, @PathParam("inputid") String inputId, @Context UriInfo uriInfo) {		
		return getInputsForScreenGet(screenId, uriInfo).stream().filter(i->i.getId().equals(inputId)).findFirst().orElse(null);
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
	
	@POST
	@Secured(right="screenInputs-write")
	@Path("/input/{id}/move")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public void moveInput(@PathParam("id") String id, int offset) {
		screenTemplateManager.moveInput(id, offset);
	}
	
	@DELETE
	@Secured(right="screenInputs-delete")
	@Path("/input/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public void deleteInput(@PathParam("id") String id) {		
		screenInputAccessor.remove(new ObjectId(id));
		screenTemplateManager.notifyChange();
	}
	
	@POST
	@Secured(right="screenInputs-write")
	@Path("/input")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public void saveInput(ScreenInput screenInput) {
		screenInputAccessor.save(screenInput);
		screenTemplateManager.notifyChange();
	}

	private Map<String, Object> getContextBindings(UriInfo uriInfo) {
		Map<String, Object> contextBindings = new HashMap<>();

		Session<User> session = getSession();
		if(session!=null) {
			contextBindings.put("user", session.getUser().getUsername());
			Role roleInContext = authorizationManager.getRoleInContext(session);
			if(roleInContext!= null) {
				String roleName = roleInContext.getAttributes().get(AbstractOrganizableObject.NAME);
				contextBindings.put("role", roleName);
			}
		}
		
		if(uriInfo != null) {
			for(String key:uriInfo.getQueryParameters().keySet()) {
				contextBindings.put(key, uriInfo.getQueryParameters().getFirst(key));
			}
		}
		return contextBindings;
	}

}
