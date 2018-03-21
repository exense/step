package step.core.deployment;

import java.util.UUID;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import step.commons.conf.Configuration;
import step.core.GlobalContext;
import step.core.access.AccessManager;
import step.core.access.Authenticator;
import step.core.access.Credentials;
import step.core.access.DefaultAuthenticator;
import step.core.access.Profile;

public class DefaultLoginProvider implements HttpLoginProvider{
	
	private static Logger logger = LoggerFactory.getLogger(DefaultLoginProvider.class);
	
	private Authenticator authenticator;
	
	private AccessServices accessServicesSingleton;
	
	private AccessManager accessManager;

	//@Override
	public void init(GlobalContext context, AccessServices accessServicesSingleton, AccessManager accessManager) {
		initAuthenticator(context);
		this.accessServicesSingleton = accessServicesSingleton;
		this.accessManager = accessManager;
	}
	
	private void initAuthenticator(GlobalContext context){
		String authenticatorClass = Configuration.getInstance().getProperty("ui.authenticator",null);
		if(authenticatorClass==null) {
			authenticator = new DefaultAuthenticator();
		} else {
			try {
				authenticator = (Authenticator) this.getClass().getClassLoader().loadClass(authenticatorClass).newInstance();
			} catch (Exception e) {
				logger.error("Error while initializing authenticator '"+authenticatorClass+"'",e);
			}
		}
		authenticator.init(context);
	}
	
	//@Override
	public Response doLogin(String request, HttpHeaders headers) {
		
        ObjectMapper om = new ObjectMapper();
		Credentials credentials = null;
		
		try {
			credentials = om.readValue(request, Credentials.class);
		} catch (Exception e) {
			logger.error("Could not read user credentials", e);
		}
		
		boolean authenticated = authenticator.authenticate(credentials);
        if(authenticated) {
        	Session session = issueToken(credentials.getUsername());
        	NewCookie cookie = new NewCookie("sessionid", session.getToken(), "/", null, 1, null, -1, null, false, false);
        	Profile profile = accessServicesSingleton.getProfile(credentials.getUsername());
        	session.setProfile(profile);
        	return Response.ok(session).cookie(cookie).build();            	
        } else {
        	return Response.status(Response.Status.UNAUTHORIZED).build();            	
        }    
        
	}
	
	//@Override
	public Session getSession(ContainerRequestContext crc, String request, HttpHeaders headers) {
		boolean useAuthentication = Configuration.getInstance().getPropertyAsBoolean("authentication", true);
		if(useAuthentication) {
			Session session = (Session) crc.getProperty("session");
			return session;			
		} else {
			return AccessServices.ANONYMOUS_SESSION;
		}
	}
	
    public Session issueToken(String username) {
    	String token = UUID.randomUUID().toString();
    	Session session = new Session();
    	session.setToken(token);
    	session.setUsername(username);
    	accessServicesSingleton.putSession(token, session);
    	return session;
    }

	@Override
	public Object getLoginInformation(Object filterInfo) {
		throw new RuntimeException("Not implemented (unused)");
	}
    

}
