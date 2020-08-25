package step.controller.grid.services;

import step.grid.TokenWrapperOwner;

public class FunctionServiceTokenWrapperOwner implements TokenWrapperOwner {

	protected String username;
	protected String ipAddress;
	protected String description;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
}
