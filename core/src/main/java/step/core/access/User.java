package step.core.access;

import java.util.List;

public class User {

	private String username;

	private String password;
	
	private List<String> roles;

	public User() {
		super();
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
