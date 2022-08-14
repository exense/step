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
package step.plugins.views;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import step.core.deployment.AbstractStepServices;

@Singleton
@Path("/views")
@Tag(name="Private View Plugin")
public class ViewPluginServices extends AbstractStepServices {

	private ViewManager viewManager;

	@PostConstruct
	public void init() throws Exception {
		super.init();
		viewManager = getContext().get(ViewManager.class);
	}

	@GET
	@Path("/{id}/{executionId}")
	@Produces(MediaType.APPLICATION_JSON)
	public ViewModel getView(@PathParam("id") String viewId, @PathParam("executionId") String executionId) {
		return viewManager.queryView(viewId, executionId);
	}
}
