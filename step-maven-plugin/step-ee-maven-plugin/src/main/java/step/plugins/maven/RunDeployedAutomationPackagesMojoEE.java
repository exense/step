package step.plugins.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import step.client.credentials.ControllerCredentials;
import step.controller.multitenancy.Tenant;
import step.controller.multitenancy.client.MultitenancyClient;
import step.controller.multitenancy.client.RemoteMultitenancyClientImpl;

@Mojo(name = "run-deployed-automation-packages-ee")
public class RunDeployedAutomationPackagesMojoEE extends AbstractRunDeployedAutomationPackagesMojo {
	@Parameter(property = "step.step-project-name")
	private String stepProjectName;

	@Parameter(property = "step.auth-token", required = false)
	private String authToken;

	public String getAuthToken() {
		return authToken;
	}

	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}

	@Override
	public void execute() throws MojoExecutionException {

		if (getStepProjectName() != null && !getStepProjectName().isEmpty()) {
			getLog().info("Step project name: " + getStepProjectName());

			TenantSwitcher tenantSwitcher = new TenantSwitcher() {
				@Override
				protected MultitenancyClient createClient() {
					return createMultitenancyClient();
				}
			};

			try {
				Tenant currentTenant = tenantSwitcher.switchTenant(getStepProjectName());
				getLog().info("Current tenant: " + currentTenant.getName() + " (" + currentTenant.getProjectId() + "). Is global: " + currentTenant.isGlobal());

			} catch (MojoExecutionException e) {
				getLog().error("Unable to switch tenant");
				throw logAndThrow(e.getMessage(), e);
			}
		}

		super.execute();
	}

	@Override
	protected ControllerCredentials getControllerCredentials() {
		return new ControllerCredentials(getUrl(), getAuthToken());
	}

	protected MultitenancyClient createMultitenancyClient() {
		return new RemoteMultitenancyClientImpl(getControllerCredentials());
	}

	public String getStepProjectName() {
		return stepProjectName;
	}

	public void setStepProjectName(String stepProjectName) {
		this.stepProjectName = stepProjectName;
	}
}