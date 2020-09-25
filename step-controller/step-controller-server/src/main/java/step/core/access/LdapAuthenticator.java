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
import step.core.controller.errorhandling.ApplicationException;

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
		String ldapFilter = configuration.getProperty("ui.authenticator.ldap.filter",null);
		String ldapTechuser = configuration.getProperty("ui.authenticator.ldap.techuser",null);
		String ldapTechpwd = configuration.getProperty("ui.authenticator.ldap.techpwd",null);
		
		// Ldap over SSL case
		String pathToJks = configuration.getProperty("ui.authenticator.ldap.ssl.pathToJks",null);
		String jksPassword = configuration.getProperty("ui.authenticator.ldap.ssl.jksPassword",null);
		
		try {
			directory = new LDAPClient(ldapUrl,ldapBaseDn,ldapFilter, ldapTechuser,ldapTechpwd, pathToJks, jksPassword);
			logger.info("LdapAuthenticator is active.");
			authenticator = new CypherAuthenticator(directory);
		} catch (NamingException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public boolean authenticate(Credentials credentials) throws Exception {
		try {
			return authenticator.authenticate(credentials);
		} catch (NamingException e) {
			e.printStackTrace();
			throw new ApplicationException(100, "Invalid username/password", null);
		}
	}
}
