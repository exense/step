package step.plugins.maven;

import ch.exense.commons.io.Poller;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

// TODO: rename 'run execution bundle on step'...
// TODO: for uploading goal 'upload keyword package'
// TODO: we can upload keywords on verify phase?
@Mojo(name = "run-execution-bundle")
public class RunExecutionBundleMojo extends AbstractStepPluginMojo {

	@Parameter(property = "step-run-exec-bundle.group-id", required = true, defaultValue = "${project.groupId}")
	private String groupId;

	@Parameter(property = "step-run-exec-bundle.artifact-id", required = true, defaultValue = "${project.artifactId}")
	private String artifactId;

	@Parameter(property = "step-run-exec-bundle.artifact-version", required = true, defaultValue = "${project.version}")
	private String artifactVersion;

	@Parameter(property = "step-run-exec-bundle.artifact-classifier", required = false)
	private String artifactClassifier;

	@Parameter(property = "step-run-exec-bundle.step-maven-settings", required = false)
	private String stepMavenSettings;

	@Parameter(property = "step-run-exec-bundle.description", required = false, defaultValue = "")
	private String description;

	@Parameter(property = "step-run-exec-bundle.user-id", required = false, defaultValue = "admin")
	private String userId;

	@Parameter(property = "step-run-exec-bundle.custom-parameters", required = false)
	private Map<String, String> customParameters;

	@Parameter(property = "step-run-exec-bundle.check-exec-result", defaultValue = "false")
	private Boolean checkExecutionResult;

	@Parameter(property = "step-run-exec-bundle.exec-result-timeout-s", defaultValue = "30")
	private Integer executionResultTimeoutS;

	@Parameter(property = "step-run-exec-bundle.exec-result-poll-period-s", defaultValue = "3")
	private Integer executionResultPollPeriodS;

	public RunExecutionBundleMojo() {
	}

	public void execute() throws MojoExecutionException {
		getLog().info("Run step executions for build " + getBuildFinalName() + " and project version " + getProjectVersion());

		HttpPost post = prepareExecutionRunRequest();

		String executionId;
		try {
			executionId = httpClient.execute(post, new BasicHttpClientResponseHandler());
			getLog().info("Execution has been registered in step: " + executionId);
		} catch (IOException ex) {
			throw new MojoExecutionException("Bad response from step: " + ex.getMessage(), ex);
		}

		if (getCheckExecutionResult()) {
			checkExecutionRunResult(executionId);
		}
	}

	private void checkExecutionRunResult(String executionId) throws MojoExecutionException {
		getLog().info("Waiting for execution result from step...");
		try {
			Poller.waitFor(() -> {
				try {
					HttpGet request = prepareExecutionResultCheckRequest(executionId);
					String response = httpClient.execute(request, new BasicHttpClientResponseHandler());
					ExecutionStatusResponseDto responseBody = objectMapper.readValue(response, ExecutionStatusResponseDto.class);

					if (!Objects.equals(responseBody.getStatus(), ExecutionStatusResponseDto.EXECUTION_FINAL_STATUS)) {
						getLog().info("Execution " + executionId + " is still in status " + responseBody.getStatus() + "...");
						return false;
					} else if (!Objects.equals(responseBody.getResult(), ExecutionStatusResponseDto.REPORT_NODE_OK_STATUS)) {
						throw new MojoExecutionException("The execution result is NOT OK for execution " + executionId + ". Final status is " + responseBody.getResult());
					} else {
						getLog().info("The execution result is OK. Final status is " + responseBody.getResult());
						return true;
					}

				} catch (MojoExecutionException e) {
					throw new MojoExceptionWrapper("Unable to check the execution status", e);
				} catch (IOException e) {
					String errMess = "Unable to get execution result from step";
					getLog().error(errMess, e);
					throw new MojoExceptionWrapper(errMess, new MojoExecutionException(errMess, e));
				}
			}, getExecutionResultTimeoutS() * 1000, getExecutionResultPollPeriodS() * 1000);
		} catch (TimeoutException | InterruptedException ex) {
			String errMess = "The success execution result is not received from step in " + getExecutionResultTimeoutS() + "seconds";
			getLog().error(errMess, ex);
			throw new MojoExecutionException(errMess, ex);
		} catch (MojoExceptionWrapper ex) {
			throw ex.getCause();
		}
	}

	protected HttpPost prepareExecutionRunRequest() throws MojoExecutionException {
		String fullUri = prepareExecutionRunUri();

		getLog().info("Calling the execution run in step: " + fullUri);

		ExecutionParametersDto bodyDto = new ExecutionParametersDto();
		bodyDto.setMode("RUN");
		bodyDto.setUserID(getUserId());
		bodyDto.setDescription(getDescription());
		bodyDto.setCustomParameters(getCustomParameters());

		RepositoryObjectReferenceDto repoRef = new RepositoryObjectReferenceDto();
		repoRef.setRepositoryID("Artifact");

		HashMap<String, String> repoParams = new HashMap<>();
		repoParams.put("groupId", getGroupId());
		repoParams.put("artifactId", getArtifactId());
		repoParams.put("version", getArtifactVersion());
		if (getArtifactClassifier() != null && !getArtifactClassifier().isEmpty()) {
			repoParams.put("classifier", getArtifactClassifier());
		}
		if (getStepMavenSettings() != null && !getStepMavenSettings().isEmpty()) {
			repoParams.put("mavenSettings", getStepMavenSettings());
		}

		repoRef.setRepositoryParameters(repoParams);

		bodyDto.setRepositoryObject(repoRef);

		String bodyString = null;
		try {
			bodyString = objectMapper.writeValueAsString(bodyDto);
		} catch (JsonProcessingException e) {
			String errMess = "Unable to prepare request body";
			getLog().error(errMess, e);
			throw new MojoExecutionException(errMess, e);
		}
		getLog().info("Request body: " + bodyString);

		HttpPost post = null;
		try {
			post = new HttpPost(new URI(fullUri));
		} catch (URISyntaxException e) {
			getLog().error("Bad URI", e);
			throw new MojoExecutionException("Unable execute bundle in step", e);
		}

		post.addHeader("Content-Type", "application/json");
		if (getAuthToken() != null && !getAuthToken().isEmpty()) {
			post.addHeader("Authorization", "Bearer " + getAuthToken());
		}
		post.setEntity(HttpEntities.create(bodyString));
		return post;
	}

	protected HttpGet prepareExecutionResultCheckRequest(String executionId) throws MojoExecutionException {
		HttpGet get = null;
		try {
			get = new HttpGet(new URI(prepareExecutionStatusUri(executionId)));
		} catch (URISyntaxException e) {
			getLog().error("Bad URI", e);
			throw new MojoExecutionException("Unable to run execution bundle in step", e);
		}
		if (getAuthToken() != null && !getAuthToken().isEmpty()) {
			get.addHeader("Authorization", "Bearer " + getAuthToken());
		}

		return get;
	}

	protected String prepareExecutionRunUri() {
		String fullUri = getUrl();
		if(!getUrl().endsWith("/")){
			fullUri += "/";
		}
		fullUri += "rest/executions/start";
		return fullUri;
	}

	protected String prepareExecutionStatusUri(String executionId) {
		String fullUri = getUrl();
		if(!getUrl().endsWith("/")){
			fullUri += "/";
		}
		fullUri += "rest/executions/" + executionId;
		return fullUri;
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

	public String getStepMavenSettings() {
		return stepMavenSettings;
	}

	public void setStepMavenSettings(String stepMavenSettings) {
		this.stepMavenSettings = stepMavenSettings;
	}

	public Boolean getCheckExecutionResult() {
		return checkExecutionResult;
	}

	public void setCheckExecutionResult(Boolean checkExecutionResult) {
		this.checkExecutionResult = checkExecutionResult;
	}

	public Integer getExecutionResultTimeoutS() {
		return executionResultTimeoutS;
	}

	public void setExecutionResultTimeoutS(Integer executionResultTimeoutS) {
		this.executionResultTimeoutS = executionResultTimeoutS;
	}

	public Integer getExecutionResultPollPeriodS() {
		return executionResultPollPeriodS;
	}

	public void setExecutionResultPollPeriodS(Integer executionResultPollPeriodS) {
		this.executionResultPollPeriodS = executionResultPollPeriodS;
	}
}