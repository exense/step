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
import step.core.authentication.AuthorizationServerManager;
import step.core.authentication.JWTSettings;
import step.core.authentication.ResourceServerManager;
import step.core.controller.ControllerSetting;
import step.core.controller.ControllerSettingAccessor;
import step.core.controller.ControllerSettingPlugin;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.framework.server.access.AccessManager;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Plugin(dependencies = ControllerSettingPlugin.class)
public class SecurityPlugin extends AbstractControllerPlugin {

	private static final Logger logger = LoggerFactory.getLogger(SecurityPlugin.class);
	
	private GlobalContext context;
	private Configuration configuration;
	private ControllerSettingAccessor settingAccessor;

	@Override
	public void serverStart(GlobalContext context) throws Exception {
		this.context = context;
		settingAccessor = context.require(ControllerSettingAccessor.class);
		this.configuration = context.getConfiguration();

		RoleProvider roleProvider = initAccessManager();
		context.put(RoleProvider.class, roleProvider);

		RoleResolver roleResolver = new RoleResolverImpl(context.getUserAccessor());
		AccessManager accessManager = new AccessManagerImpl(roleProvider, roleResolver);
		context.put(AccessManager.class, accessManager);
		
		JWTSettings jwtSettings = new JWTSettings(context.getConfiguration(),getOrInitSecret( ));
		AuthorizationServerManager authorizationServerManager = initAuthorizationServerManager(jwtSettings, accessManager);
		ResourceServerManager resourceServerManager = new ResourceServerManager(jwtSettings, authorizationServerManager);
		context.put(AuthorizationServerManager.class, authorizationServerManager);
		context.put(ResourceServerManager.class, resourceServerManager);

		Authenticator authenticator = initAuthenticator();
		AuthenticationManager authenticationManager = new AuthenticationManager(configuration, authenticator, context.getUserAccessor(),
				authorizationServerManager);
		context.put(AuthenticationManager.class, authenticationManager);
	}

	@Override
	public void migrateData(GlobalContext context) throws Exception {

	}

	@Override
	public void initializeData(GlobalContext context) throws Exception {

	}

	@Override
	public void afterInitializeData(GlobalContext context) throws Exception {

	}

	@Override
	public void serverStop(GlobalContext context) {

	}

	private String getOrInitSecret() throws NoSuchAlgorithmException {
		ControllerSetting secretSetting = settingAccessor.getSettingByKey("authenticator.jwt.secret");
		if (secretSetting == null || secretSetting.getValue() == null) {
			KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
			keyGenerator.init(128);
			SecretKey secretKey = keyGenerator.generateKey();
			byte[] rawData = secretKey.getEncoded();
			String encodedKey = Base64.getEncoder().encodeToString(rawData);
			secretSetting = new ControllerSetting("authenticator.jwt.secret", encodedKey);
			settingAccessor.save(secretSetting);
		} 
		return secretSetting.getValue();
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

	private AuthorizationServerManager initAuthorizationServerManager(JWTSettings jwtSettings, AccessManager accessManager) throws Exception {
		AuthorizationServerManager authorizationServerManager;
		String authorizationServerManagerClass = configuration.getProperty("authenticator.class","step.core.authentication.AuthorizationServerManagerLocal");
		try {
			authorizationServerManager = (AuthorizationServerManager) this.getClass().getClassLoader().loadClass(authorizationServerManagerClass)
					.getConstructor(JWTSettings.class,AccessManager.class).newInstance(jwtSettings,accessManager);
		} catch (Exception e) {
			logger.error("Error while initializing authenticator '" + authorizationServerManagerClass + "'", e);
			throw e;
		}
		return authorizationServerManager;
	}
}
