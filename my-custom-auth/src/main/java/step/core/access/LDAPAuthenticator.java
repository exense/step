package step.core.access;

import javax.naming.NamingException;
import javax.ws.rs.core.HttpHeaders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.GlobalContext;
import step.core.access.Credentials;
import step.core.deployment.HttpAuthenticator;

public class LDAPAuthenticator implements HttpAuthenticator{
	
	private static Logger logger = LoggerFactory.getLogger(LDAPAuthenticator.class);
	
	private LDAPClient client;
	
	public void init(GlobalContext context) {

        final String ldapServer = context.getConfiguration().getProperty("ui.authenticator.ldap.url");
        final String ldapBaseDn = context.getConfiguration().getProperty("ui.authenticator.ldap.baseDn");
        
        final String ldapUsername = context.getConfiguration().getProperty("ui.authenticator.ldap.techuser");
        final String ldapPassword = context.getConfiguration().getProperty("ui.authenticator.ldap.techpwd");
        final String cypher = context.getConfiguration().getProperty("ui.authenticator.ldap.cypher");

		try {
			this.client = new LDAPClient(ldapServer,ldapBaseDn,ldapUsername,ldapPassword, cypher);
		} catch (NamingException e) {
			logger.error("Could not initialize LDAP Client", e);
		}
	}

	@Override
	public boolean authenticate(String request, HttpHeaders headers){
        
		try {
			// Step 2 : perform actual authentication (user-password based in the case of LDAP)
			Credentials credentials = extractCredentials(request, headers);
			return this.client.authenticate(credentials.getUsername(), credentials.getPassword());
		}catch (Exception e) {
			logger.debug("Error while authenticating user: " + e);
			return false;
		}
	}

	@Override
	public String extractUsername(String request, HttpHeaders headers){
		return extractCredentials(request, headers).getUsername();
	}
	
	private Credentials extractCredentials(String request, HttpHeaders headers){
		ObjectMapper om = new ObjectMapper();
		Credentials credentials = null;
		
		try {
			credentials = om.readValue(request, Credentials.class);
		} catch (Exception e) {
			logger.error("Could not read user credentials", e);
		}
		return credentials;
	}
}
