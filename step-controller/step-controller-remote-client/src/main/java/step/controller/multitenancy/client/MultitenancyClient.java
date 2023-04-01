package step.controller.multitenancy.client;

import java.io.Closeable;
import java.util.List;

import step.controller.multitenancy.Tenant;

public interface MultitenancyClient extends Closeable {

	public void selectTenant(String tenantName) throws Exception;
	
	public List<Tenant> getAvailableTenants();
	
	public Tenant getCurrentTenant();
}
