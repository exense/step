package step.plugins.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import step.client.credentials.ControllerCredentials;

public abstract class AbstractStepPluginMojo extends AbstractMojo {

	@Parameter(property = "step.url", required = true)
	private String url;

	@Parameter(defaultValue = "${project.build.finalName}", readonly = true)
	private String buildFinalName;

	@Parameter(defaultValue = "${project.version}", readonly = true)
	private String projectVersion;

	@Parameter(property = "step.auth-token", required = false)
	private String authToken;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getBuildFinalName() {
		return buildFinalName;
	}

	public void setBuildFinalName(String buildFinalName) {
		this.buildFinalName = buildFinalName;
	}

	public String getProjectVersion() {
		return projectVersion;
	}

	public void setProjectVersion(String projectVersion) {
		this.projectVersion = projectVersion;
	}

	public String getAuthToken() {
		return authToken;
	}

	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}

	protected void logAndThrow(String errorText, Throwable e) throws MojoExecutionException {
		getLog().error(errorText, e);
		throw new MojoExecutionException(errorText, e);
	}

	protected ControllerCredentials getControllerCredentials(){
		return new ControllerCredentials(getUrl(), getAuthToken());
	}
}
