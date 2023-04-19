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
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import step.core.GlobalContext;
import step.core.controller.ApplicationConfiguration;
import step.core.controller.ApplicationConfigurationManager;
import step.core.controller.SessionResponseBuilder;
import step.core.plugins.AbstractWebPlugin;
import step.framework.server.Session;
import step.framework.server.security.Secured;

@Singleton
@Path("/app")
@Tag(name="Private Application")
public class ApplicationServices extends AbstractStepServices {

	private WebApplicationConfigurationManager webApplicationConfigurationManager;
	private ApplicationConfigurationManager applicationConfigurationManager;
	private SessionResponseBuilder sessionResponseBuilder;

	public ApplicationServices() {
		super();
	}

	@PostConstruct
	public void init() throws Exception {
		super.init();
		GlobalContext context = getContext();
		webApplicationConfigurationManager = getContext().require(WebApplicationConfigurationManager.class);
		applicationConfigurationManager = Objects.requireNonNullElse(context.get(ApplicationConfigurationManager.class),
				new ApplicationConfigurationManager());
		sessionResponseBuilder = getContext().require(SessionResponseBuilder.class);

	}


	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/plugins")
	public List<AbstractWebPlugin> getWebPlugins() {
		return getContext().getControllerPluginManager().getWebPlugins();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/conf")
	public ApplicationConfiguration getApplicationConfiguration() {
		ApplicationConfiguration conf = applicationConfigurationManager.getDefaultBuilder(configuration)
						.putMiscParam("enforceschemas", getContext().getConfiguration().getProperty("enforceschemas", "false"))
								.putMiscParams(webApplicationConfigurationManager.getConfiguration(getSession())).build();
		return conf;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("repositories/parameters")
	public Set<String> getAllRepositoriesCanonicalParameters() {
		return getContext().getRepositoryObjectManager().getAllRepositoriesCanonicalParameters();
	}

	@GET
	@Secured
	@Path("/session")
	public Map<String, Object> getCurrentSession() {
		Session session = Objects.requireNonNullElse(getSession(),new Session());
		return sessionResponseBuilder.build(session);
	}

}
