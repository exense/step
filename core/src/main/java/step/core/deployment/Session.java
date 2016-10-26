package step.core.deployment;

public class Session {
	
	String username;
	
	String token;
	
	long lasttouch;

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
