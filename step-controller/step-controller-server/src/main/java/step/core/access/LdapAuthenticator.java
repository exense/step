package step.core.access;

import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.commons.auth.Authenticator;
import ch.commons.auth.Credentials;
import ch.commons.auth.PasswordDirectory;
import ch.commons.auth.cyphers.CypherAuthenticator;
import ch.commons.auth.ldap.LDAPClient;
import ch.exense.commons.app.Configuration;
import step.core.GlobalContext;
import step.core.GlobalContextAware;

public class LdapAuthenticator implements Authenticator, GlobalContextAware {
	
	private static Logger logger = LoggerFactory.getLogger(LdapAuthenticator.class);
	
	private Configuration configuration;
	
	private PasswordDirectory directory;
	private CypherAuthenticator authenticator;

	@Override
	public void setGlobalContext(GlobalContext context) {
		configuration = context.getConfiguration();
		
		String ldapUrl = configuration.getProperty("ui.authenticator.ldap.url",null);
		String ldapBaseDn = configuration.getProperty("ui.authenticator.ldap.base",null);
		String ldapTechuser = configuration.getProperty("ui.authenticator.ldap.techuser",null);
		String ldapTechpwd = configuration.getProperty("ui.authenticator.ldap.techpwd",null);
		
		try {
			directory = new LDAPClient(ldapUrl,ldapBaseDn,ldapTechuser,ldapTechpwd);
			logger.info("LdapAuthenticator is active.");
		} catch (NamingException e) {
			e.printStackTrace();
		}
		authenticator = new CypherAuthenticator(directory);
	}

	@Override
	public boolean authenticate(Credentials credentials) throws Exception {
		return authenticator.authenticate(credentials);
	}
}
