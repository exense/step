package step.plugins.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import step.client.credentials.ControllerCredentials;
import step.controller.multitenancy.Tenant;
import step.controller.multitenancy.client.RemoteMultitenancyClientImpl;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Mojo(name = "upload-keywords-package-ee")
public class UploadKeywordsPackageMojoEE extends AbstractUploadKeywordsPackageMojo {

	@Parameter(property = "step-upload-keywords.step-project-name")
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

		if (getStepProjectName() != null && !getStepProjectName().isBlank()) {
			getLog().info("Step project name: " + getStepProjectName());

			// setup step project and use it to search fo existing packages
			try (RemoteMultitenancyClientImpl multitenancyClient = createMultitenancyClient()) {
				List<Tenant> availableTenants = multitenancyClient.getAvailableTenants();
				Tenant currentTenant = null;
				if (availableTenants != null) {
					currentTenant = availableTenants.stream().filter(t -> Objects.equals(t.getName(), getStepProjectName())).findFirst().orElse(null);
				}
				if (currentTenant == null) {
					throw new MojoExecutionException("Unable to resolve tenant by name: " + getStepProjectName());
				}
				multitenancyClient.selectTenant(getStepProjectName());

				getLog().info("Current tenant: " + currentTenant.getName() + " (" + currentTenant.getProjectId() + "). Is global: " + currentTenant.isGlobal());
				searchCriteria.put("attributes.project", currentTenant.getProjectId());

			} catch (IOException e) {
				logAndThrow("Unable to use multitenancy client", e);
			} catch (Exception e) {
				logAndThrow("Unable to use tenant: " + getStepProjectName(), e);
			}

		}

	}

	protected RemoteMultitenancyClientImpl createMultitenancyClient() {
		return new RemoteMultitenancyClientImpl(getControllerCredentials());
	}
}
