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

import ch.commons.auth.Authenticator;
import ch.commons.auth.Credentials;
import ch.exense.commons.app.Configuration;
import org.ldaptive.*;
import org.ldaptive.auth.AuthenticationRequest;
import org.ldaptive.auth.AuthenticationResponse;
import org.ldaptive.auth.SearchDnResolver;
import org.ldaptive.auth.SimpleBindAuthenticationHandler;
import org.ldaptive.control.ResponseControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.GlobalContext;
import step.core.GlobalContextAware;

public class LdapAuthenticator implements Authenticator, GlobalContextAware {
	
	private static Logger logger = LoggerFactory.getLogger(LdapAuthenticator.class);
	
	private Configuration configuration;
	private String ldapUrl;
	private String ldapBaseDn;
	private String ldapFilter;

	private String ldapTechUser;
	private String ldapTechPwd;

	private Boolean ldapTLS;

	@Override
	public void setGlobalContext(GlobalContext context) {
		configuration = context.getConfiguration();
		
		ldapUrl = configuration.getProperty("ui.authenticator.ldap.url",null);
		ldapBaseDn = configuration.getProperty("ui.authenticator.ldap.base",null);
		ldapFilter = configuration.getProperty("ui.authenticator.ldap.filter",null);

		ldapTechUser = configuration.getProperty("ui.authenticator.ldap.techuser",null);
		ldapTechPwd = configuration.getProperty("ui.authenticator.ldap.techpwd",null);

		ldapTLS = Boolean.parseBoolean(configuration.getProperty("ui.authenticator.ldap.tls","false"));
		
		// Ldap certificate
		//pathToJks = configuration.getProperty("ui.authenticator.ldap.ssl.pathToJks",null);
		//jksPassword = configuration.getProperty("ui.authenticator.ldap.ssl.jksPassword",null);
	}

	@Override
	public boolean authenticate(Credentials credentials) throws Exception {
		ConnectionConfig.Builder builder = ConnectionConfig.builder()
				.url(ldapUrl).useStartTLS(ldapTLS);

		if (ldapTechUser!=null) {
			builder = builder.connectionInitializers(new BindConnectionInitializer(ldapTechUser, new Credential(ldapTechPwd)));
		}

		ConnectionConfig connConfig = builder.build();

		// use a search dn resolver
		SearchDnResolver dnResolver = SearchDnResolver.builder()
				.factory(new DefaultConnectionFactory(connConfig))
				.dn(ldapBaseDn)
				.filter(ldapFilter)
				.build();

		SimpleBindAuthenticationHandler authHandler = new SimpleBindAuthenticationHandler(new DefaultConnectionFactory(connConfig));

		org.ldaptive.auth.Authenticator auth = new org.ldaptive.auth.Authenticator(dnResolver, authHandler);
		AuthenticationResponse response = auth.authenticate(
				new AuthenticationRequest(credentials.getUsername(), new Credential(credentials.getPassword()), null));
		if (response.isSuccess()) {
			return true;
		} else {
			return false;
		}
	}
}
