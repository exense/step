package step.core.access;

import java.util.ArrayList;
import java.util.List;

import ch.exense.commons.app.Configuration;
import step.core.deployment.Session;

public class AuthenticationManager {

	private final Configuration configuration;
	private final Authenticator authenticator;
	private final UserAccessor userAccessor;
	private final List<AuthenticationManagerListener> listeners = new ArrayList<>();

	public AuthenticationManager(Configuration configuration, Authenticator authenticator, UserAccessor userAccessor) {
		super();
		this.configuration = configuration;
		this.authenticator = authenticator;
		this.userAccessor = userAccessor;
	}

	public boolean useAuthentication() {
		return configuration.getPropertyAsBoolean("authentication", true);
	}

	public boolean authenticate(Session session, Credentials credentials) {
		boolean authenticated = authenticator.authenticate(credentials);
		if (authenticated) {
			setUserToSession(session, credentials.getUsername());
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
		session.setAuthenticated(true);
		User user = userAccessor.getByUsername(username);
		session.setUser(user);
	}
	
	protected void logoutSession(Session session) {
		session.setUser(null);
		session.setAuthenticated(false);
	}

	public void authenticateDefaultUserIfAuthenticationIsDisabled(Session session) {
		if (!session.isAuthenticated() && !useAuthentication()) {
			setUserToSession(session, "admin");
		}
	}

	public boolean registerListener(AuthenticationManagerListener e) {
		return listeners.add(e);
	}
	
	public static interface AuthenticationManagerListener {
		
		public void onSuccessfulAuthentication(Session session);
	}
}
