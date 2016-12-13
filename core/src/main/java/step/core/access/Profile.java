package step.core.access;

import java.util.List;

public class Profile {
	
	private List<String> rights;
	
	private String role;

	public Profile() {
		super();
	}

	public List<String> getRights() {
		return rights;
	}

	public void setRights(List<String> rights) {
		this.rights = rights;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}
}
