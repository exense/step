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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.commons.auth.Authenticator;
import ch.exense.commons.app.Configuration;
import step.core.GlobalContext;
import step.core.GlobalContextAware;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;

@Plugin
public class SecurityPlugin extends AbstractControllerPlugin {

	private static final Logger logger = LoggerFactory.getLogger(SecurityPlugin.class);
	
	private GlobalContext context;
	private Configuration configuration;
	
	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		this.context = context;
		this.configuration = context.getConfiguration();
		
		Authenticator authenticator = initAuthenticator();
		AuthenticationManager authenticationManager = new AuthenticationManager(configuration, authenticator, context.getUserAccessor());
		context.put(AuthenticationManager.class, authenticationManager);
		
		RoleProvider roleProvider = initAccessManager();
		context.put(RoleProvider.class, roleProvider);

		RoleResolver roleResolver = new RoleResolverImpl(context.getUserAccessor());
		AccessManager accessManager = new AccessManagerImpl(roleProvider, roleResolver);
		context.put(AccessManager.class, accessManager);
		
		super.executionControllerStart(context);
	}

	private Authenticator initAuthenticator() throws Exception {
		Authenticator authenticator;
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
		if(authenticator instanceof GlobalContextAware) {
			((GlobalContextAware) authenticator).setGlobalContext(context);
		}
		return authenticator;
	}
	
	private RoleProvider initAccessManager() throws Exception {
		RoleProvider roleProvider;
		String accessManagerClass = configuration.getProperty("ui.roleprovider",null);
		if(accessManagerClass==null) {
			roleProvider = new DefaultRoleProvider();
		} else {
			try {
				roleProvider = (RoleProvider) this.getClass().getClassLoader().loadClass(accessManagerClass).newInstance();
			} catch (Exception e) {
				logger.error("Error while initializing access manager '"+accessManagerClass+"'",e);
				throw e;
			}
		}
		if(roleProvider instanceof GlobalContextAware) {
			((GlobalContextAware) roleProvider).setGlobalContext(context);
		}
		return roleProvider;
	}
}
