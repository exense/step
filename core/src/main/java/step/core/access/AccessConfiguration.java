package step.core.access;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccessConfiguration {
	
	boolean authentication;
	
	boolean demo;
	
	List<String> roles;
	
	Map<String,String> miscParams;

	public AccessConfiguration() {
		super();
		this.miscParams = new HashMap<>();
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

	public Map<String, String> getMiscParams() {
		return miscParams;
	}

	public void setMiscParams(Map<String, String> miscParams) {
		this.miscParams = miscParams;
	}
}
