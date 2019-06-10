package step.core.deployment;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.exense.commons.app.Configuration;
import step.core.access.AccessConfiguration;
import step.core.access.AccessManager;
import step.core.access.Authenticator;
import step.core.access.Credentials;
import step.core.access.DefaultAccessManager;
import step.core.access.DefaultAuthenticator;
import step.core.access.Profile;

@Singleton
@Path("/access")
public class AccessServices extends AbstractServices {
	
	private static Logger logger = LoggerFactory.getLogger(AccessServices.class);
	
	public static final String AUTHENTICATION_SERVICE = "AuthenticationService";
		
	private ConcurrentHashMap<String, Session> sessions;
	
	private Timer sessionExpirationTimer; 
	
	private Authenticator authenticator;
	
	private AccessManager accessManager;
	
	public AccessServices() {
		super();
		sessions = new ConcurrentHashMap<>();
	}
	
	@PostConstruct
	public void init() throws Exception {
		super.init();
		controller.getContext().put(AUTHENTICATION_SERVICE, this);
		
		configuration = controller.getContext().getConfiguration();
		
		initAuthenticator();
		initAccessManager();
		
		sessionExpirationTimer = new Timer("Session expiration timer");
		sessionExpirationTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				final int sessionTimeout = configuration.getPropertyAsInteger("ui.sessiontimeout.minutes", 180)*60000;
				long time = System.currentTimeMillis();
				sessions.entrySet().removeIf(entry->(entry.getValue().lasttouch+sessionTimeout)<time);
			}
		}, 60000, 60000);
	}
	
	@PreDestroy
	private void close() {
		if(sessionExpirationTimer != null) {
			sessionExpirationTimer.cancel();			
		}
	}

	private void initAuthenticator() throws Exception {
		String authenticatorClass = configuration.getProperty("ui.authenticator",null);
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
	
	private void initAccessManager() throws Exception {
		String accessManagerClass = configuration.getProperty("ui.accessmanager",null);
		if(accessManagerClass==null) {
			accessManager = new DefaultAccessManager();
		} else {
			try {
				accessManager = (AccessManager) this.getClass().getClassLoader().loadClass(accessManagerClass).newInstance();
			} catch (Exception e) {
				logger.error("Error while initializing access manager '"+accessManagerClass+"'",e);
				throw e;
			}
		}
		accessManager.init(getContext());
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
	
	@POST
	@Secured
	@Path("/logout")
    public void logout(@Context ContainerRequestContext crc) {
		Session session = (Session) crc.getProperty("session");
		if(session != null) {
			sessions.remove(session.getToken());
		}
    }
	
	@GET
	@Path("/conf")
	public AccessConfiguration getAccessConfiguration() {
		AccessConfiguration conf = new AccessConfiguration();
		conf.setDemo(isDemo());
		conf.setAuthentication(useAuthentication());
		conf.setRoles(accessManager.getRoles());
		
		// conf should cover more than just AccessConfiguration but we'll store the info here for right now
		Configuration ctrlConf = getContext().getConfiguration();
		conf.getMiscParams().put("enforceschemas", getContext().getConfiguration().getProperty("enforceschemas", "false"));
		if(ctrlConf.hasProperty("logo.loginpage")) {
			conf.getMiscParams().put("logologinpage", ctrlConf.getProperty("logo.loginpage"));			
		}
		if(ctrlConf.hasProperty("logo.main")) {
			conf.getMiscParams().put("logomain", ctrlConf.getProperty("logo.main"));			
		}
		if(ctrlConf.hasProperty("ui.default.url")) {
			conf.setDefaultUrl(ctrlConf.getProperty("ui.default.url"));
		}
		return conf;
	}
	
	public boolean useAuthentication() {
		return configuration.getPropertyAsBoolean("authentication", true);
	}
	
	public boolean isDemo() {
		return configuration.getPropertyAsBoolean("demo", false);
	}
	
	static Session ANONYMOUS_SESSION = new Session();
	{
		ANONYMOUS_SESSION.setUsername("admin");
		Profile profile = new Profile();
		profile.setRole("default");
		ANONYMOUS_SESSION.setProfile(profile);
	}
	
	@GET
	@Secured
	@Path("/session")
	public Session getSession(@Context ContainerRequestContext crc) {
		boolean useAuthentication = configuration.getPropertyAsBoolean("authentication", true);
		if(useAuthentication) {
			Session session = (Session) crc.getProperty("session");
			return session;			
		} else {
			return ANONYMOUS_SESSION;
		}
	}
	
	//Not "Secured" on purpose:
	//we're allow third parties to loosely check the validity of a token in an SSO context
	@GET
	@Path("/checkToken")
	public Boolean isValidToken(@QueryParam("token") String token) {
		boolean useAuthentication = configuration.getPropertyAsBoolean("authentication", true);
		if(useAuthentication) {
			try {
				validateAndTouchToken(token);
			} catch (TokenValidationException e) {
				logger.debug("Token " + token + " is invalid.");
				return false;
			}
			logger.debug("Token " + token + " is valid.");
			return true;
		} else {
			return true;
		}
	}

	private Profile getProfile(String username) {
		Profile profile = new Profile();
		List<String> rights = accessManager.getRights(username);
		profile.setRights(rights);
		profile.setRole(accessManager.getRole(username));
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
    
    public Session validateAndTouchToken(String token) throws TokenValidationException {
    	Session session = sessions.get(token);
    	if(session != null)
    		session.touch();
    	else
    		throw new TokenValidationException("Session with token '" + token + "' is invalid");

    	return session;
    }
}
