package step.plugins.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import step.client.credentials.ControllerCredentials;
import step.controller.multitenancy.Tenant;
import step.controller.multitenancy.client.MultitenancyClient;
import step.controller.multitenancy.client.RemoteMultitenancyClientImpl;

import java.util.Map;

@Mojo(name = "upload-keywords-package-ee")
public class UploadKeywordsPackageMojoEE extends AbstractUploadKeywordsPackageMojo {

	@Parameter(property = "step.step-project-name")
	private String stepProjectName;

	@Parameter(property = "step.auth-token", required = false)
	private String authToken;

	public String getStepProjectName() {
		return stepProjectName;
	}

	public void setStepProjectName(String stepProjectName) {
		this.stepProjectName = stepProjectName;
	}

	public String getAuthToken() {
		return authToken;
	}

	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}

	@Override
	protected ControllerCredentials getControllerCredentials() {
		return new ControllerCredentials(getUrl(), getAuthToken());
	}

	@Override
	protected void fillAdditionalPackageSearchCriteria(Map<String, String> searchCriteria) throws MojoExecutionException {
		super.fillAdditionalPackageSearchCriteria(searchCriteria);

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

				// setup Step project and use it to search fo existing packages
				searchCriteria.put("attributes.project", currentTenant.getProjectId());
			} catch (Exception e) {
				getLog().error("Unable to switch tenant");
				throw logAndThrow(e.getMessage(), e);
			}
		}
	}

	protected MultitenancyClient createMultitenancyClient() {
		return new RemoteMultitenancyClientImpl(getControllerCredentials());
	}
}
