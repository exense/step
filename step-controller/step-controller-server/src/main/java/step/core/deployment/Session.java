package step.core.deployment;

import step.core.AbstractContext;
import step.core.access.Profile;

public class Session extends AbstractContext {
	
	protected String username;
	
	protected Profile profile;
	
	protected String token;
	
	protected long lasttouch;

	public Session() {
		super();
		touch();
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Profile getProfile() {
		return profile;
	}

	public void setProfile(Profile profile) {
		this.profile = profile;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
	
	public void touch() {
		lasttouch = System.currentTimeMillis();
	}

}
