package step.core.access;

import step.core.accessors.AbstractIdentifiableObject;

public class User extends AbstractIdentifiableObject {
	
	private String username;

	private String password;
	
	private String role;
	
	private Preferences preferences;

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

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public Preferences getPreferences() {
		return preferences;
	}

	public void setPreferences(Preferences preferences) {
		this.preferences = preferences;
	}
}
