package step.plugins.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import step.client.executions.RemoteExecutionFuture;
import step.client.executions.RemoteExecutionManager;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.repositories.RepositoryObjectReference;

import java.util.*;
import java.util.concurrent.TimeoutException;

public abstract class AbstractRunAutomationPackagesMojo extends AbstractStepPluginMojo {
	@Parameter(property = "step-run-auto-packages.group-id", required = true, defaultValue = "${project.groupId}")
	private String groupId;
	@Parameter(property = "step-run-auto-packages.artifact-id", required = true, defaultValue = "${project.artifactId}")
	private String artifactId;
	@Parameter(property = "step-run-auto-packages.artifact-version", required = true, defaultValue = "${project.version}")
	private String artifactVersion;
	@Parameter(property = "step-run-auto-packages.artifact-classifier", required = false)
	private String artifactClassifier;
	@Parameter(property = "step-run-auto-packages.description", required = false)
	private String description;
	@Parameter(property = "step-run-auto-packages.execution-parameters", required = false)
	private Map<String, String> executionParameters;
	@Parameter(property = "step-run-auto-packages.exec-result-timeout-s", defaultValue = "30")
	private Integer executionResultTimeoutS;
	@Parameter(property = "step-run-auto-packages.wait-for-exec", defaultValue = "true")
	private Boolean waitForExecution;
	@Parameter(property = "step-run-auto-packages.ensure-exec-success", defaultValue = "true")
	private Boolean ensureExecutionSuccess;

	protected void executeBundleOnStep(Map<String, Object> executionContext) throws MojoExecutionException {
		String executionId = null;
		try (RemoteExecutionManager remoteExecutionManager = createRemoteExecutionManager()) {
			ExecutionParameters executionParameters = prepareExecutionParameters(executionContext);

			executionId = remoteExecutionManager.execute(executionParameters);
			getLog().info("Execution has been registered in Step: " + executionId);

			if (getWaitForExecution()) {
				waitForExecutionFinish(remoteExecutionManager, executionId);
			}
		} catch (Exception ex) {
			throw logAndThrow("Unable to run execution in Step", ex);
		}
	}

	protected ExecutionParameters prepareExecutionParameters(Map<String, Object> executionContext) {
		ExecutionParameters executionParameters = new ExecutionParameters();
		executionParameters.setMode(ExecutionMode.RUN);
		executionParameters.setDescription(getDescription());
		executionParameters.setCustomParameters(getExecutionParameters());
		executionParameters.setRepositoryObject(prepareExecutionRepositoryObject(executionContext));
		return executionParameters;
	}

	protected RemoteExecutionManager createRemoteExecutionManager() {
		return new RemoteExecutionManager(getControllerCredentials());
	}

	protected abstract RepositoryObjectReference prepareExecutionRepositoryObject(Map<String, Object> executionContext);

	private void waitForExecutionFinish(RemoteExecutionManager remoteExecutionManager, String executionId) throws MojoExecutionException {
		getLog().info("Waiting for execution result from Step...");

		// run the execution and wait until it is finished
		try {
			RemoteExecutionFuture executionFuture = remoteExecutionManager.getFuture(executionId).waitForExecutionToTerminate(getExecutionResultTimeoutS() * 1000);
			Execution endedExecution = executionFuture.getExecution();
			if (getEnsureExecutionSuccess() && !Objects.equals(endedExecution.getResult(), ReportNodeStatus.PASSED)) {
				throw new MojoExecutionException("The execution result is NOT OK for execution " + executionId + ". Final status is " + endedExecution.getResult());
			} else {
				getLog().info("The execution result is OK. Final status is " + endedExecution.getResult());
			}
		} catch (TimeoutException | InterruptedException ex) {
			throw logAndThrow("The success execution result is not received from Step in " + getExecutionResultTimeoutS() + "seconds", ex);
		}
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

	public Map<String, String> getExecutionParameters() {
		return executionParameters;
	}

	public void setExecutionParameters(Map<String, String> executionParameters) {
		this.executionParameters = executionParameters;
	}

	public Integer getExecutionResultTimeoutS() {
		return executionResultTimeoutS;
	}

	public void setExecutionResultTimeoutS(Integer executionResultTimeoutS) {
		this.executionResultTimeoutS = executionResultTimeoutS;
	}

	public Boolean getWaitForExecution() {
		return waitForExecution;
	}

	public void setWaitForExecution(Boolean waitForExecution) {
		this.waitForExecution = waitForExecution;
	}

	public Boolean getEnsureExecutionSuccess() {
		return ensureExecutionSuccess;
	}

	public void setEnsureExecutionSuccess(Boolean ensureExecutionSuccess) {
		this.ensureExecutionSuccess = ensureExecutionSuccess;
	}

}
