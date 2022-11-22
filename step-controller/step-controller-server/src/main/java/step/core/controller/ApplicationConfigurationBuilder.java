package step.core.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationConfigurationBuilder {
	private boolean authentication;
	private String authenticatorName;
	private boolean demo;
	private boolean debug;
	private boolean noLoginMask;
	private boolean passwordManagement;
	private boolean userManagement;
	private List<String> roles;
	private Map<String, String> miscParams = new HashMap<>();
	private String defaultUrl;
	private String title;


	public ApplicationConfigurationBuilder setAuthentication(boolean authentication) {
		this.authentication = authentication;
		return this;
	}

	public ApplicationConfigurationBuilder setAuthenticatorName(String authenticatorName) {
		this.authenticatorName = authenticatorName;
		return this;
	}

	public ApplicationConfigurationBuilder setDemo(boolean demo) {
		this.demo = demo;
		return this;
	}

	public ApplicationConfigurationBuilder setDebug(boolean debug) {
		this.debug = debug;
		return this;
	}

	public ApplicationConfigurationBuilder setNoLoginMask(boolean noLoginMask) {
		this.noLoginMask = noLoginMask;
		return this;
	}

	public ApplicationConfigurationBuilder setUserManagement(boolean userManagement) {
		this.userManagement = userManagement;
		return this;
	}

	public ApplicationConfigurationBuilder setPasswordManagement(boolean passwordManagement) {
		this.passwordManagement = passwordManagement;
		return this;
	}

	public ApplicationConfigurationBuilder setRoles(List<String> roles) {
		this.roles = roles;
		return this;
	}

	public ApplicationConfigurationBuilder putMiscParam(String key, String value) {
		this.miscParams.put(key, value);
		return this;
	}

	public ApplicationConfigurationBuilder putMiscParams(Map<String, String> miscParams) {
		this.miscParams.putAll(miscParams);
		return this;
	}

	public ApplicationConfigurationBuilder setDefaultUrl(String defaultUrl) {
		this.defaultUrl = defaultUrl;
		return this;
	}

	public ApplicationConfigurationBuilder setTitle(String title) {
		this.title = title;
		return this;
	}

	public ApplicationConfiguration build() {
		return new ApplicationConfiguration(authentication, authenticatorName, demo, debug, noLoginMask,
				passwordManagement, userManagement, roles, miscParams, defaultUrl, title);
	}
}