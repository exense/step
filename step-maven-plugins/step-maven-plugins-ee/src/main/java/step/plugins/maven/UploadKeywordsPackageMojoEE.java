package step.plugins.maven;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import step.client.credentials.ControllerCredentials;

@Mojo(name = "upload-keywords-package-ee")
public class UploadKeywordsPackageMojoEE extends AbstractUploadKeywordsPackageMojo {

	@Parameter(property = "step-upload-keywords.step-project-id")
	private String stepProjectId;

	@Parameter(property = "step.auth-token", required = false)
	private String authToken;

	public String getStepProjectId() {
		return stepProjectId;
	}

	public void setStepProjectId(String stepProjectId) {
		this.stepProjectId = stepProjectId;
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

}
