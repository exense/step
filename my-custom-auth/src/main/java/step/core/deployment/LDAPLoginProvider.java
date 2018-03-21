package step.core.deployment;

import java.util.Arrays;

import javax.naming.NamingException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.GlobalContext;
import step.core.access.AccessManager;
import step.core.access.Credentials;
import step.core.access.Profile;

public class LDAPLoginProvider implements HttpLoginProvider{
	
	private static Logger logger = LoggerFactory.getLogger(LDAPLoginProvider.class);
	
	private LDAPClient client;
	

	private AccessServices accessServicesSingleton;
	
	private AccessManager accessManager;
	

	public void init(GlobalContext context, AccessServices accessServices, AccessManager accessManager) {

		this.accessServicesSingleton = accessServices;
		this.accessManager = accessManager;
		
        final String ldapServer = context.getConfiguration().getProperty("authentication.ldap.url");
        final String ldapBaseDn = context.getConfiguration().getProperty("authentication.ldap.baseDn");
        
        final String ldapUsername = context.getConfiguration().getProperty("authentication.ldap.techuser");
        final String ldapPassword = context.getConfiguration().getProperty("authentication.ldap.techpwd");
        final String cypher = context.getConfiguration().getProperty("authentication.ldap.cypher");

		try {
			this.client = new LDAPClient(ldapServer,ldapBaseDn,ldapUsername,ldapPassword, cypher);
		} catch (NamingException e) {
			logger.error("Could not initialize LDAP Client", e);
		}
	}

	public Response doLogin(String request, HttpHeaders headers) {
        
		ObjectMapper om = new ObjectMapper();
		Credentials credentials = null;
		
		try {
			credentials = om.readValue(request, Credentials.class);
		} catch (Exception e) {
			logger.error("Could not read user credentials", e);
		}
		
		try {
			
			if(this.client.authenticate(credentials.getUsername(), credentials.getPassword())) {
				//TODO: load profile
				
				Session session = new Session();
				session.setUsername(credentials.getUsername());
				Profile p = new Profile();
				p.setRights(Arrays.asList(new String[]{"interactive","plan-read","plan-write","plan-delete","plan-execute","kw-read","kw-write","kw-delete","kw-execute","report-read","user-write","user-read","task-read","task-write","task-delete","admin","param-read","param-write","param-delete"}));
				p.setRole("admin");
				session.setProfile(p);
				session.setToken(credentials.getUsername());
				session.setUsername(credentials.getUsername());
				
				this.accessServicesSingleton.putStepSession(credentials.getUsername(), session);
				
				return Response.ok(session).build();
			}
			else
				return Response.status(Response.Status.UNAUTHORIZED).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.UNAUTHORIZED).build();
		}
	}

	public Session getSession(ContainerRequestContext crc, String request, HttpHeaders headers) {
		return null;
	}

	public Object getLoginInformation(Object filterInfo) {
		// TODO Auto-generated method stub
		return null;
	}

}
