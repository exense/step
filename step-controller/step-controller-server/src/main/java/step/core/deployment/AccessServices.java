package step.core.deployment;

import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.commons.auth.Credentials;
import ch.exense.commons.app.Configuration;
import step.core.GlobalContext;
import step.core.access.AccessConfiguration;
import step.core.access.AccessManager;
import step.core.access.AuthenticationManager;
import step.core.access.Role;
import step.core.access.RoleProvider;
import step.core.access.User;
import step.core.accessors.AbstractOrganizableObject;

@Singleton
@Path("/access")
public class AccessServices extends AbstractServices {
	private static Logger logger = LoggerFactory.getLogger(AccessServices.class);
	
	private RoleProvider roleProvider;
	private AuthenticationManager authenticationManager;
	private AccessManager accessManager;
	
	public AccessServices() {
		super();
	}
	
	@PostConstruct
	public void init() throws Exception {
		super.init();
		GlobalContext context = controller.getContext();
		
		roleProvider = context.get(RoleProvider.class);
		authenticationManager = context.get(AuthenticationManager.class);
		accessManager = context.get(AccessManager.class);
	}
	
	public static class SessionResponse {
		
		private String username;
		private Role role;
		
		public SessionResponse(String username, Role role) {
			super();
			this.username = username;
			this.role = role;
		}

		public String getUsername() {
			return username;
		}

		public Role getRole() {
			return role;
		}
	}

	@POST
	@Path("/login")
    @Produces("application/json")
    @Consumes("application/json")
    public Response authenticateUser(Credentials credentials) {
		Session session = getSession();
		boolean authenticated = false;
		try {
			authenticated = authenticationManager.authenticate(session, credentials);
		}catch(Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).entity("Authentication failed. Check the server logs for more details.").type("text/plain").build();
		}
        if(authenticated) {
        	SessionResponse sessionResponse = buildSessionResponse(session);
        	return Response.ok(sessionResponse).build();            	
        } else {
        	return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).entity("Invalid username/password").type("text/plain").build();
        }    
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
		User user = session.getUser();
		Role role = accessManager.getRoleInContext(session);
		return new SessionResponse(user.getUsername(), role);
	}
	
	@GET
	@Path("/conf")
	public AccessConfiguration getAccessConfiguration() {
		AccessConfiguration conf = new AccessConfiguration();
		conf.setDemo(isDemo());
		conf.setAuthentication(authenticationManager.useAuthentication());
		conf.setRoles(roleProvider.getRoles().stream().map(r->r.getAttributes().get(AbstractOrganizableObject.NAME)).collect(Collectors.toList()));
		
		// conf should cover more than just AccessConfiguration but we'll store the info here for right now
		Configuration ctrlConf = getContext().getConfiguration();
		conf.getMiscParams().put("enforceschemas", getContext().getConfiguration().getProperty("enforceschemas", "false"));

		if(ctrlConf.hasProperty("ui.default.url")) {
			conf.setDefaultUrl(ctrlConf.getProperty("ui.default.url"));
		}
		conf.setDebug(ctrlConf.getPropertyAsBoolean("ui.debug", false));
		conf.setTitle(ctrlConf.getProperty("ui.title", "STEP"));
		return conf;
	}
	
	@POST
	@Secured
	@Path("/logout")
    public void logout() {
		setSession(null);
    }
	
	public boolean isDemo() {
		return configuration.getPropertyAsBoolean("demo", false);
	}
}
