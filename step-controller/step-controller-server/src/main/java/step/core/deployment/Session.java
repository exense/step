package step.core.deployment;

import step.core.AbstractContext;
import step.core.access.User;

public class Session extends AbstractContext {
	
	protected boolean authenticated;
	
	protected User user;
	
	public Session() {
		super();
	}

	public boolean isAuthenticated() {
		return authenticated;
	}

	public void setAuthenticated(boolean authenticated) {
		this.authenticated = authenticated;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
}
