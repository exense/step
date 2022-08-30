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


import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Context;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.commons.auth.Credentials;
import ch.exense.commons.app.Configuration;
import step.core.GlobalContext;
import step.core.access.AccessConfiguration;
import step.framework.server.access.AccessManager;
import step.core.access.AuthenticationManager;
import step.core.access.Role;
import step.core.access.RoleProvider;
import step.core.access.User;
import step.core.accessors.AbstractOrganizableObject;
import step.core.authentication.AuthorizationServerManager;
import step.core.controller.errorhandling.ApplicationException;
import step.framework.server.Session;
import step.framework.server.audit.AuditLogger;
import step.framework.server.security.Secured;

import static step.core.authentication.JWTSettings.CONFIG_KEY_JWT_NOLOGIN;

@Singleton
@Path("/access")
@Tag(name = "Access")
public class AccessServices extends AbstractStepServices {
	private static Logger logger = LoggerFactory.getLogger(AccessServices.class);
	
	private RoleProvider roleProvider;
	private AuthenticationManager authenticationManager;
	private AccessManager accessManager;
	private AuthorizationServerManager authorizationServerManager;
	private WebApplicationConfigurationManager applicationConfigurationManager;

	public AccessServices() {
		super();
	}
	
	@PostConstruct
	public void init() throws Exception {
		super.init();
		GlobalContext context = getContext();
		
		roleProvider = context.get(RoleProvider.class);
		authenticationManager = context.get(AuthenticationManager.class);
		accessManager = context.get(AccessManager.class);
		authorizationServerManager = context.get(AuthorizationServerManager.class);
		applicationConfigurationManager = getContext().require(WebApplicationConfigurationManager.class);
	}
	
	public static class SessionResponse {
		
		private String username;
		private Role role;
		private boolean otp;
		
		public SessionResponse(String username, Role role, boolean otp) {
			super();
			this.username = username;
			this.role = role;
			this.otp = otp;
		}

		public String getUsername() {
			return username;
		}

		public Role getRole() {
			return role;
		}

		public boolean isOtp() {
			return otp;
		}
	}

	@POST
	@Path("/login")
    @Produces("application/json")
    @Consumes("application/json")
    public Response authenticateUser(Credentials credentials) {
		Session<User> session = getSession();
		boolean authenticated = false;
		try {
			authenticated = authenticationManager.authenticate(session, credentials);
		} catch(ApplicationException e) { // in our case all application exception are authorization error, to be refactored
			return Response.status(Response.Status.FORBIDDEN.getStatusCode()).entity("Authorization failed: "+e.getErrorMessage()).type("text/plain").build();
		}catch(Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).entity("Authentication failed. Check the server logs for more details.").type("text/plain").build();
		}
        if(authenticated) {
			return Response.ok(null).build();
        } else {
        	return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).entity("Authentication failed: Invalid username/password").type("text/plain").build();
        }    
    }
	
	public static class TokenResponse {
		String token;

		public String getToken() {
			return token;
		}

		public void setToken(String token) {
			this.token = token;
		}
	}

	@GET
	@Secured
	@Path("/service-account/token")
	@Produces(MediaType.TEXT_PLAIN)
	public String getServiceAccountToken(@QueryParam("lifetime") long days) {
		Session session = getSession();
		return authorizationServerManager.getServiceAccountToken(session, days);
	}


	@GET
	@Secured
	@Path("/session")
	public SessionResponse getCurrentSession() {
		Session session = getSession();
		return buildSessionResponse(session);
	}
	
	// TODO Reimplement this method as it isn't working anymore since the sessions are now managed by the web server
	//Not "Secured" on purpose:
	//we're allow third parties to loosely check the validity of a token in an SSO context
//	@GET
//	@Path("/checkToken")
//	public Boolean isValidToken(@QueryParam("token") String token) {
//		logger.debug("Token " + token + " is valid.");
//		return true;
//	}

	protected SessionResponse buildSessionResponse(Session session) {
		User user = (User) session.getUser();
		Role role = accessManager.getRoleInContext(session);
		boolean isOtp = (authenticationManager.implementOTP()) ?
				(boolean) Objects.requireNonNullElse(session.getUser().getCustomField("otp"), false):
				false;
		return new SessionResponse(user.getUsername(), role, isOtp);
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/conf")
	public AccessConfiguration getAccessConfiguration() {
		AccessConfiguration conf = new AccessConfiguration();
		conf.setDemo(isDemo());
		conf.setAuthentication(authenticationManager.useAuthentication());
		conf.setAuthenticatorName(authenticationManager.getAuthenticatorName());
		conf.setNoLoginMask(configuration.getPropertyAsBoolean(CONFIG_KEY_JWT_NOLOGIN, false));
		conf.setRoles(roleProvider.getRoles().stream().map(r->r.getAttributes().get(AbstractOrganizableObject.NAME)).collect(Collectors.toList()));
		
		// conf should cover more than just AccessConfiguration but we'll store the info here for right now
		Configuration ctrlConf = getContext().getConfiguration();
		Map<String, String> miscParams = conf.getMiscParams();
		miscParams.put("enforceschemas", getContext().getConfiguration().getProperty("enforceschemas", "false"));
		miscParams.putAll(applicationConfigurationManager.getConfiguration(getSession()));

		if(ctrlConf.hasProperty("ui.default.url")) {
			conf.setDefaultUrl(ctrlConf.getProperty("ui.default.url"));
		}
		conf.setDebug(ctrlConf.getPropertyAsBoolean("ui.debug", false));
		conf.setTitle(ctrlConf.getProperty("ui.title", "STEP"));
		
		conf.setDisplayLegacyPerfDashboard(ctrlConf.getPropertyAsBoolean("ui.performance.dashboard.legacy.enabled",true));
		conf.setDisplayNewPerfDashboard(ctrlConf.getPropertyAsBoolean("ui.performance.dashboard.beta.enabled",true));
		return conf;
	}
	
	@POST
	@Secured
	@Path("/logout")
    public void logout(@Context HttpServletRequest req) {
		AuditLogger.log(req, 200);//must be called before invalidation
		invalidateSession();
    }
	
	public boolean isDemo() {
		return configuration.getPropertyAsBoolean("demo", false);
	}
}
