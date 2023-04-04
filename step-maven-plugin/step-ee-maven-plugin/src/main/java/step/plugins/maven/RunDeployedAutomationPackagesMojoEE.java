package step.plugins.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import step.client.credentials.ControllerCredentials;
import step.controller.multitenancy.Tenant;
import step.controller.multitenancy.client.MultitenancyClient;
import step.controller.multitenancy.client.RemoteMultitenancyClientImpl;
import step.core.execution.model.ExecutionParameters;

import java.util.Map;

@Mojo(name = "run-deployed-automation-packages-ee")
public class RunDeployedAutomationPackagesMojoEE extends AbstractRunDeployedAutomationPackagesMojo {
	@Parameter(property = "step.step-project-name", required = true)
	private String stepProjectName;

	@Parameter(property = "step-run-auto-packages.user-id", required = false)
	private String userId;

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
	protected ExecutionParameters prepareExecutionParameters(Map<String, Object> executionContext) {
		ExecutionParameters res = super.prepareExecutionParameters(executionContext);
		res.setUserID(getUserId());
		return res;
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

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
}