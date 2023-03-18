package step.controller.multitenancy.client;

import java.util.List;

import step.controller.multitenancy.Tenant;

public interface MultitenancyClient {

	public void selectTenant(String tenantName) throws Exception;
	
	public List<Tenant> getAvailableTenants();
	
	public Tenant getCurrentTenant();
}
