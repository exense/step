package step.plugins.maven;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import step.client.credentials.ControllerCredentials;

@Mojo(name = "run-deployed-execution-bundle-ee")
public class RunDeployedExecutionBundleMojoEE extends AbstractRunDeployedExecutionBundleMojo {
	@Parameter(property = "step.auth-token", required = false)
	private String authToken;

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