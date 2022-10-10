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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import step.core.auth.Authenticator;
import step.core.auth.Credentials;
import ch.exense.commons.app.Configuration;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.authentication.AuthenticationFilter;
import step.core.authentication.AuthenticationTokenDetails;
import step.core.authentication.AuthorizationServerManager;
import step.core.authentication.ResourceServerManager;
import step.core.controller.errorhandling.ApplicationException;
import step.framework.server.Session;

public class AuthenticationManager {

	private static Logger logger = LoggerFactory.getLogger(AuthenticationManager.class);

	private final Configuration configuration;
	private final Authenticator authenticator;
	private final UserAccessor userAccessor;
	private final List<AuthenticationManagerListener> listeners = new ArrayList<>();

	private final AuthorizationServerManager authorizationServerManager;
	private final ResourceServerManager resourceServerManager;

	public AuthenticationManager(Configuration configuration, Authenticator authenticator, UserAccessor userAccessor,
								 AuthorizationServerManager authorizationServerManager, ResourceServerManager resourceServerManager) {
		super();
		this.configuration = configuration;
		this.authenticator = authenticator;
		this.userAccessor = userAccessor;
		this.authorizationServerManager = authorizationServerManager;
		this.resourceServerManager = resourceServerManager;
	}

	public boolean implementOTP() {
		return (authenticator.getClass().isAssignableFrom(DefaultAuthenticator.class));
	}

	public boolean useAuthentication() {
		return configuration.getPropertyAsBoolean("authentication", true);
	}
	
	public String getAuthenticatorName(){
		return authenticator.getClass().getSimpleName();
	}

	public boolean authenticate(Session<User> session, Credentials credentials) throws Exception {
		boolean authenticated = authenticator.authenticate(credentials);
		if (authenticated) {
			setUserToSession(session, credentials.getUsername());
			try {
				listeners.forEach(l->l.onSuccessfulAuthentication(session));
			} catch(Exception e) {
				logoutSession(session);
				throw e;
			}
			authorizationServerManager.getAccessToken(session, null, null); // TODO to be added as part of onSuccessfulAuthentication for default and LDAP auth
			return true;
		} else {
			return false;
		}
	}

	public boolean authenticate(Session<User> session, String code, String sessionState) throws Exception {
		String token = authorizationServerManager.getAccessToken(session, code, sessionState);
		AuthenticationTokenDetails authenticationTokenDetails = resourceServerManager.parseAndValidateToken(token, session);
		logger.info("authenticationTokenDetails: " + authenticationTokenDetails);
		String user = createUserIfRequired(session, authenticationTokenDetails);
		if (user != null) {
			setUserToSession(session, user);
			try {
				listeners.forEach(l->l.onSuccessfulAuthentication(session));
			} catch(Exception e) {
				logoutSession(session);
				throw e;
			}
			return true;
		} else {
			return false;
		}
	}

	protected void setUserToSession(Session session, String username) {
		User user = userAccessor.getByUsername(username);
		
		if(user == null) {
			throw new ApplicationException(100, "Unknow user '"+username+"': this user is not defined in step", null);
		}
		session.setAuthenticated(true);
		session.setUser(user);
	}
	
	protected void logoutSession(Session session) {
		session.setUser(null);
		session.setAuthenticated(false);
		session.setToken(null);
		session.setLocalToken(false);
	}

	public synchronized void authenticateDefaultUserIfAuthenticationIsDisabled(Session session) {
		if (!session.isAuthenticated() && !useAuthentication()) {
			User user = userAccessor.getByUsername("admin");
			if(user == null) {
				user = defaultAdminUser();
				userAccessor.save(user);
			}
			
			setUserToSession(session, "admin");
			authorizationServerManager.getAccessToken(session, null, null);
		}
	}
	
	public static User defaultAdminUser() {
		User user = new User();
		user.setUsername("admin");
		user.setRole("admin");
		user.setPassword(UserAccessorImpl.encryptPwd("init"));
		return user;
	}

	public String resetPwd(User user) {
		if (authenticator.getClass().isAssignableFrom(LdapAuthenticator.class)) {
			throw new RuntimeException("Password management is not supported by the LdapAuthenticator");
		} else {
			String pwd = generateSecureRandomPassword();
			user.setPassword(encryptPwd(pwd));
			user.addCustomField("otp", true);
			return pwd;
		}
	}

	public String encryptPwd(String pwd) {
		return DigestUtils.sha512Hex(pwd);
	}

	private String generateSecureRandomPassword() {
		Stream<Character> pwdStream = Stream.concat(getRandomChars(2, 48, 57), Stream.concat(getRandomChars(2, 33, 45),
				Stream.concat(getRandomChars(4, 65, 90), getRandomChars(4, 97, 122))));
		List<Character> charList = pwdStream.collect(Collectors.toList());
		Collections.shuffle(charList);
		String password = charList.stream().collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
				.toString();
		return password;
	}

	private Stream<Character> getRandomChars(int count, int lowerBound, int upperBound) {
		Random random = new SecureRandom();
		IntStream specialChars = random.ints(count, lowerBound, upperBound);
		return specialChars.mapToObj(data -> (char) data);
	}

	public boolean registerListener(AuthenticationManagerListener e) {
		return listeners.add(e);
	}
	
	public static interface AuthenticationManagerListener {
		
		public void onSuccessfulAuthentication(Session<User> session);
	}

	private String createUserIfRequired(Session s, AuthenticationTokenDetails authenticationTokenDetails) {
		String username = authenticationTokenDetails.getUsername();
		User byUsername = userAccessor.getByUsername(username);
		if (byUsername == null) {
			User user = new User();
			user.setUsername(username);
			user.setRole("admin");
			userAccessor.save(user);
		}
		return username;
	}
}
