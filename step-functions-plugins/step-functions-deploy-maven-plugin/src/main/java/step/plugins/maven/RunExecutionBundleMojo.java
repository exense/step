package step.plugins.maven;

import ch.exense.commons.io.Poller;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import step.client.executions.RemoteExecutionManager;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;
import step.core.repositories.RepositoryObjectReference;

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
		getLog().info("Run step execution for deployed module " + getBuildFinalName() + " (version=" + getProjectVersion() + ")");

		String executionId = null;
		try (RemoteExecutionManager remoteExecutionManager = new RemoteExecutionManager(getControllerCredentials())) {
			ExecutionParameters executionParameters = new ExecutionParameters();
			executionParameters.setMode(ExecutionMode.RUN);
			executionParameters.setUserID(getUserId());
			executionParameters.setDescription(getDescription());
			executionParameters.setCustomParameters(getCustomParameters());
			executionParameters.setRepositoryObject(new RepositoryObjectReference("Artifact", prepareRepositoryParameters()));

			executionId = remoteExecutionManager.execute(executionParameters);
			getLog().info("Execution has been registered in step: " + executionId);

			if (getCheckExecutionResult()) {
				checkExecutionRunResult(remoteExecutionManager, executionId);
			}
		} catch (Exception ex) {
			logAndThrow("Unable to run execution in step", ex);
		}
	}

	private void checkExecutionRunResult(RemoteExecutionManager remoteExecutionManager, String executionId) throws MojoExecutionException {
		getLog().info("Waiting for execution result from step...");
		try {
			Poller.waitFor(() -> {
				try {
					Execution execution = remoteExecutionManager.getFuture(executionId).getExecution();

					if (!Objects.equals(execution.getStatus(), ExecutionStatus.ENDED)) {
						getLog().info("Execution " + executionId + " is still in status " + execution.getStatus() + "...");
						return false;
					} else if (!Objects.equals(execution.getResult(), ReportNodeStatus.PASSED)) {
						throw new MojoExecutionException("The execution result is NOT OK for execution " + executionId + ". Final status is " + execution.getResult());
					} else {
						getLog().info("The execution result is OK. Final status is " + execution.getResult());
						return true;
					}
				} catch (MojoExecutionException e) {
					throw new MojoExceptionWrapper("Unable to check the execution status", e);
				}
			}, getExecutionResultTimeoutS() * 1000, getExecutionResultPollPeriodS() * 1000);
		} catch (TimeoutException | InterruptedException ex) {
			logAndThrow("The success execution result is not received from step in " + getExecutionResultTimeoutS() + "seconds", ex);
		}
	}

	private HashMap<String, String> prepareRepositoryParameters() {
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
		return repoParams;
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