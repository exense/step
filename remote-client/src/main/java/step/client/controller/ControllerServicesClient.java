package step.client.controller;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;

import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;

public class ControllerServicesClient extends AbstractRemoteClient {

	public ControllerServicesClient() {
		super();
	}

	public ControllerServicesClient(ControllerCredentials credentials) {
		super(credentials);
	}

	public void shutdownController() {
		Builder r = requestBuilder("/rest/controller/shutdown");
		executeRequest(()->r.post(Entity.entity(null, MediaType.APPLICATION_JSON)));
	}
	
	public void changeMyPassword(String newPwd) {
		Builder r = requestBuilder("/rest/admin/myaccount/changepwd");
		executeRequest(()->r.post(Entity.entity("{\"newPwd\" : \""+newPwd+"\"}", MediaType.APPLICATION_JSON)));
	}
}
