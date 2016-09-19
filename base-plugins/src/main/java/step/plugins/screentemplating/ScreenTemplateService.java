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

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import step.core.deployment.AbstractServices;

@Singleton
@Path("screens")
public class ScreenTemplateService extends AbstractServices {
	
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Input> getInputsForScreen(@PathParam("id") String screenId, @Context UriInfo uriInfo) {
		Map<String, Object> contextBindings = getContextBindings(uriInfo);
		ScreenTemplatePlugin plugin = (ScreenTemplatePlugin) getContext().get(ScreenTemplatePlugin.SCREEN_TEMPLATE_KEY);
		return plugin.getInputsForScreen(screenId, contextBindings);
	}

	private Map<String, Object> getContextBindings(UriInfo uriInfo) {
		Map<String, Object> contextBindings = new HashMap<>();
		for(String key:uriInfo.getQueryParameters().keySet()) {
			contextBindings.put(key, uriInfo.getQueryParameters().getFirst(key));
		}
		return contextBindings;
	}

}
