package step.controller.multitenancy.client;

import java.util.List;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;

import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.controller.multitenancy.Tenant;

public class RemoteMultitenancyClientImpl extends AbstractRemoteClient implements MultitenancyClient {

	public RemoteMultitenancyClientImpl(ControllerCredentials credentials) {
		super(credentials);
	}

	@Override
	public void selectTenant(String tenantName) throws Exception {
		Builder b = requestBuilder("/rest/tenants/current");
		executeRequest(()->b.post(Entity.entity(tenantName, MediaType.APPLICATION_JSON)));
	}
	
	@Override
	public List<Tenant> getAvailableTenants() {
		Builder b = requestBuilder("/rest/tenants");
		return executeRequest(()->b.get(new GenericType<List<Tenant>>() {}));
	}
	
	@Override
	public Tenant getCurrentTenant() {
		Builder b = requestBuilder("/rest/tenants/current");
		return executeRequest(()->b.get(Tenant.class));
	}
}
