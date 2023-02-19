package step.plugins.maven;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@Mojo(name = "step-functions-deploy")
public class StepFunctionsDeployMojo extends AbstractMojo {

	@Parameter(property = "step-functions-deploy.url", required = true)
	private String url;

	@Parameter(property = "step-functions-deploy.group-id", required = true, defaultValue = "${project.groupId}")
	private String groupId;

	@Parameter(property = "step-functions-deploy.artifact-id", required = true, defaultValue = "${project.artifactId}")
	private String artifactId;

	@Parameter(property = "step-functions-deploy.artifact-version", required = true, defaultValue = "${project.version}")
	private String artifactVersion;

	@Parameter(property = "step-functions-deploy.artifact-classifier", required = false)
	private String artifactClassifier;

	@Parameter(property = "step-functions-deploy.step-maven-settings", required = false)
	private String stepMavenSettings;

	@Parameter(property = "step-functions-deploy.description", required = false, defaultValue = "")
	private String description;

	@Parameter(property = "step-functions-deploy.user-id", required = false, defaultValue = "admin")
	private String userId;

	@Parameter(property = "step-functions-deploy.custom-parameters", required = false)
	private Map<String, String> customParameters;

	@Parameter(defaultValue = "${project.build.finalName}", readonly = true)
	private String buildFinalName;

	@Parameter(defaultValue = "${project.version}", readonly = true)
	private String projectVersion;

	// TODO: security token?

	private final CloseableHttpClient httpClient = HttpClients.createDefault();

	private final ObjectMapper objectMapper = new ObjectMapper();

	public StepFunctionsDeployMojo() {
	}

	public void execute() throws MojoExecutionException {
		getLog().info("Deploy step functions for build " + buildFinalName + " and project version " + projectVersion);
		try {
			String fullUri = prepareFullUri();
			getLog().info("Sending the package to " + fullUri);

			ExecutionParametersDto bodyDto = new ExecutionParametersDto();
			bodyDto.setMode("RUN");
			bodyDto.setUserID(userId);
			bodyDto.setDescription(description);
			bodyDto.setCustomParameters(customParameters);

			RepositoryObjectReferenceDto repoRef = new RepositoryObjectReferenceDto();
			repoRef.setRepositoryID("Artifact");

			HashMap<String, String> repoParams = new HashMap<>();
			repoParams.put("groupId", groupId);
			repoParams.put("artifactId", artifactId);
			repoParams.put("version", artifactVersion);
			if (artifactClassifier != null && !artifactClassifier.isEmpty()) {
				repoParams.put("classifier", artifactClassifier);
			}
			if(stepMavenSettings != null && !stepMavenSettings.isEmpty()){
				repoParams.put("mavenSettings", stepMavenSettings);
			}

			repoRef.setRepositoryParameters(repoParams);

			bodyDto.setRepositoryObject(repoRef);

			String bodyString = objectMapper.writeValueAsString(bodyDto);
			getLog().info("Request body: " + bodyString);

			// TODO: cookie?
			HttpPost post = new HttpPost(new URI(fullUri));
			post.addHeader("Content-Type", "application/json");
//			post.addHeader("Cookie", "sessionid=node0bgesd8tlzwkijv4lkf9q2acn0.node0");
			post.setEntity(HttpEntities.create(bodyString));

			try (CloseableHttpResponse response = httpClient.execute(post)){
				getLog().info("Response received: " + response.getCode());
				if (response.getCode() != 200) {
					throw new MojoExecutionException("Bad response status from step: " + response.getCode());
				}

				getLog().info("Execution has been registered in step: " + EntityUtils.toString(response.getEntity()));
			}

		} catch (URISyntaxException | IOException | ParseException e) {
			getLog().error("Unable to deploy the package to step", e);
			throw new MojoExecutionException("Unable to deploy the package to step", e);
		}
	}

	private String prepareFullUri() {
		String fullUri = url;
		if(!url.endsWith("/")){
			fullUri += "/";
		}
		fullUri += "rest/executions/start";
		return fullUri;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public String getArtifactVersion() {
		return artifactVersion;
	}

	public void setArtifactVersion(String artifactVersion) {
		this.artifactVersion = artifactVersion;
	}

	public String getArtifactClassifier() {
		return artifactClassifier;
	}

	public void setArtifactClassifier(String artifactClassifier) {
		this.artifactClassifier = artifactClassifier;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Map<String, String> getCustomParameters() {
		return customParameters;
	}

	public void setCustomParameters(Map<String, String> customParameters) {
		this.customParameters = customParameters;
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

	public String getStepMavenSettings() {
		return stepMavenSettings;
	}

	public void setStepMavenSettings(String stepMavenSettings) {
		this.stepMavenSettings = stepMavenSettings;
	}
}