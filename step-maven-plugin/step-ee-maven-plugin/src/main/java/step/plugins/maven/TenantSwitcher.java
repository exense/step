package step.plugins.maven;

import org.apache.maven.plugin.MojoExecutionException;
import step.controller.multitenancy.Tenant;
import step.controller.multitenancy.client.MultitenancyClient;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public abstract class TenantSwitcher {

	public Tenant switchTenant(String projectName) throws MojoExecutionException {
		try (MultitenancyClient multitenancyClient = createClient()) {
			List<Tenant> availableTenants = multitenancyClient.getAvailableTenants();

			Tenant currentTenant = null;
			if (availableTenants != null) {
				currentTenant = availableTenants.stream().filter(t -> Objects.equals(t.getName(), projectName)).findFirst().orElse(null);
			}
			if (currentTenant == null) {
				throw new MojoExecutionException("Unable to resolve tenant by name: " + projectName);
			}
			multitenancyClient.selectTenant(projectName);
			return currentTenant;
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	protected abstract MultitenancyClient createClient();
}
