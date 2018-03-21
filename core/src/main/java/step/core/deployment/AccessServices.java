package step.core.deployment;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.commons.conf.Configuration;
import step.core.access.AccessConfiguration;
import step.core.access.AccessManager;
import step.core.access.DefaultAccessManager;
import step.core.access.Profile;

@Singleton
@Path("/access")
public class AccessServices extends AbstractServices{
	
	private static Logger logger = LoggerFactory.getLogger(AccessServices.class);
		
	private ConcurrentHashMap<String, Session> sessions;
	
	private Timer sessionExpirationTimer; 
	
	private AccessManager accessManager;
	
	private HttpLoginProvider httpLoginProvider;
	
	public static final String AUTHENTICATION_SERVICE = "AuthenticationService";
	
	public AccessServices() {
		super();
		sessions = new ConcurrentHashMap<>();
	}
	
	public void putStepSession(String key, Session session) {
		this.sessions.put(key, session);
	}
	
	public Session getStepSession(String key) {
		return this.sessions.get(key);
	}
	
	@PostConstruct
	private void init() throws Exception {
		controller.getContext().put(AUTHENTICATION_SERVICE, this);

		initAccessManager();
		initLoginProvider();
		
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

	private void initLoginProvider() {
		String loginProviderClass = controller.getContext().getConfiguration().getProperty("auth.httpLoginProviderClass", "step.core.deployment.DefaultLoginProvider");
		
		Class<?> c;
		
		try {
			c = Class.forName(loginProviderClass);
			httpLoginProvider = (HttpLoginProvider)c.getConstructor().newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		httpLoginProvider.init(controller.getContext(), this, accessManager);
	}

	private void initAccessManager() throws Exception {
		String accessManagerClass = Configuration.getInstance().getProperty("ui.accessmanager",null);
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
    public Response authenticateUser(String request, @Context HttpHeaders headers) {
		return httpLoginProvider.doLogin(request, headers);
    }
	
    public Object getLoginInformation(Object filterInfo) {
		return httpLoginProvider.getLoginInformation(filterInfo);
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
		return conf;
	}
	
	public static boolean useAuthentication() {
		return Configuration.getInstance().getPropertyAsBoolean("authentication", true);
	}
	
	public static boolean isDemo() {
		return Configuration.getInstance().getPropertyAsBoolean("demo", false);
	}
	
	@GET
	@Secured
	@Path("/session")
	public Session getSession(@Context ContainerRequestContext crc, String request, @Context HttpHeaders headers) {
		return httpLoginProvider.getSession(crc, request, headers);
	}
	
	static Session ANONYMOUS_SESSION = new Session();
	{
		ANONYMOUS_SESSION.setUsername("admin");
		Profile profile = new Profile();
		profile.setRole("default");
		ANONYMOUS_SESSION.setProfile(profile);
	}
	
	public Profile getProfile(String username) {
		Profile profile = new Profile();
		List<String> rights = accessManager.getRights(username);
		profile.setRights(rights);
		profile.setRole(accessManager.getRole(username));
		return profile;
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
