package step.core.deployment;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.commons.conf.Configuration;
import step.core.access.Authenticator;
import step.core.access.Credentials;
import step.core.access.DefaultAuthenticator;
import step.core.access.Profile;

@Singleton
@Path("/access")
public class AccessServices extends AbstractServices {
	
	private static Logger logger = LoggerFactory.getLogger(AccessServices.class);
	
	public static final String AUTHENTICATION_SERVICE = "AuthenticationService";
	
	public static List<String> roleHierarchy = Arrays.asList(new String[]{"guest","executor","developer","admin"});
	
	private ConcurrentHashMap<String, Session> sessions;
	
	private Timer sessionExpirationTimer; 
	
	private Authenticator authenticator;
	
	public AccessServices() {
		super();
		sessions = new ConcurrentHashMap<>();
	}
	
	@PostConstruct
	private void init() throws Exception {
		controller.getContext().put(AUTHENTICATION_SERVICE, this);
		
		initAuthenticator();

		sessionExpirationTimer = new Timer("Session expiration timer");
		sessionExpirationTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				final int sessionTimeout = Configuration.getInstance().getPropertyAsInteger("ui.sessiontimeout.minutes", 180)*60000;
				long time = System.currentTimeMillis();
				sessions.entrySet().removeIf(entry->(entry.getValue().lasttouch+sessionTimeout)<time);
			}
		}, 60000, 60000);
	}

	private void initAuthenticator() throws Exception {
		String authenticatorClass = Configuration.getInstance().getProperty("ui.authenticator",null);
		if(authenticatorClass==null) {
			authenticator = new DefaultAuthenticator();
		} else {
			try {
				authenticator = (Authenticator) this.getClass().getClassLoader().loadClass(authenticatorClass).newInstance();
			} catch (Exception e) {
				logger.error("Error while initializing authenticator '"+authenticatorClass+"'",e);
				throw e;
			}
		}
		authenticator.init(getContext());
	}

	@POST
	@Path("/login")
    @Produces("application/json")
    @Consumes("application/json")
    public Response authenticateUser(Credentials credentials) {
        boolean authenticated = authenticator.authenticate(credentials);
        if(authenticated) {
        	Session session = issueToken(credentials.getUsername());
        	NewCookie cookie = new NewCookie("sessionid", session.getToken(), "/", null, 1, null, -1, null, false, false);
        	Profile profile = getProfile(credentials.getUsername());
        	session.setProfile(profile);
        	return Response.ok(session).cookie(cookie).build();            	
        } else {
        	return Response.status(Response.Status.UNAUTHORIZED).build();            	
        }    
    }
	
	@GET
	@Secured
	@Path("/session")
	public Session getSession(@Context ContainerRequestContext crc) {
		Session session = (Session) crc.getProperty("session");
		return session;
	}

	private Profile getProfile(String username) {
		String role = authenticator.getRole(username);
		Profile profile = new Profile();
		profile.setRole(role);
		return profile;
	}

    private Session issueToken(String username) {
    	String token = UUID.randomUUID().toString();
    	Session session = new Session();
    	session.setToken(token);
    	session.setUsername(username);
    	sessions.put(token, session);
    	return session;
    }
    
    public Session validateAndTouchToken(String token) {
    	Session session = sessions.get(token);
    	session.touch();
    	return session;
    }
}
