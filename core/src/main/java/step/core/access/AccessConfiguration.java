package step.core.access;

import java.util.List;

public class AccessConfiguration {
	
	boolean authentication;
	
	boolean demo;
	
	List<String> roles;

	public AccessConfiguration() {
		super();
	}

	public boolean isDemo() {
		return demo;
	}

	public void setDemo(boolean demo) {
		this.demo = demo;
	}

	public boolean isAuthentication() {
		return authentication;
	}

	public void setAuthentication(boolean authentication) {
		this.authentication = authentication;
	}

	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

}
