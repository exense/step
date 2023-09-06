/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
	@Parameter(property = "step-run-auto-packages.exec-result-timeout-s", defaultValue = "3600")
	private Integer executionResultTimeoutS;
	@Parameter(property = "step-run-auto-packages.wait-for-exec", defaultValue = "true")
	private Boolean waitForExecution;
	@Parameter(property = "step-run-auto-packages.ensure-exec-success", defaultValue = "true")
	private Boolean ensureExecutionSuccess;

	@Parameter(property = "step-run-auto-packages.lib-artifact-group-id")
	private String libArtifactGroupId;

	@Parameter(property = "step-run-auto-packages.lib-artifact-id")
	private String libArtifactId;

	@Parameter(property = "step-run-auto-packages.lib-artifact-version")
	private String libArtifactVersion;

	@Parameter(property = "step-run-auto-packages.lib-artifact-classifier")
	private String libArtifactClassifier;

	@Parameter(property = "step-upload-keywords.lib-tracking-attr", required = false)
	private String libTrackingAttr;

	protected void executeBundleOnStep(Map<String, Object> executionContext) throws MojoExecutionException {
		String executionId = null;
		try (RemoteExecutionManager remoteExecutionManager = createRemoteExecutionManager()) {
			ExecutionParameters executionParameters = prepareExecutionParameters(executionContext);

			executionId = remoteExecutionManager.execute(executionParameters);
			String baseMessage = "Execution has been started in Step (" + getUrl() + "): " + executionId;

			if (getWaitForExecution()) {
				getLog().info(baseMessage + ". Waiting on results...");
				waitForExecutionFinish(remoteExecutionManager, executionId);
			} else {
				getLog().info(baseMessage + ". Waiting on results is disabled.");
			}
		} catch (Exception ex) {
			throw logAndThrow("Unable to run execution in Step (" + getUrl() + ")", ex);
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
		getLog().info("Waiting for execution result from Step (" + getUrl() + ")...");

		// run the execution and wait until it is finished
		try {
			RemoteExecutionFuture executionFuture = remoteExecutionManager.getFuture(executionId).waitForExecutionToTerminate(getExecutionResultTimeoutS() * 1000);
			Execution endedExecution = executionFuture.getExecution();
			if (getEnsureExecutionSuccess() && !Objects.equals(endedExecution.getResult(), ReportNodeStatus.PASSED)) {
				if (!endedExecution.getImportResult().isSuccessful()) {
					throw new MojoExecutionException("The execution result is NOT OK for execution " + executionId + ". The following error(s) occurred during import " +
							String.join(";", endedExecution.getImportResult().getErrors()));
				} else {
					throw new MojoExecutionException("The execution result is NOT OK for execution " + executionId + ". Final status is " + endedExecution.getResult());
				}
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

	public String getLibArtifactGroupId() {
		return libArtifactGroupId;
	}

	public void setLibArtifactGroupId(String libArtifactGroupId) {
		this.libArtifactGroupId = libArtifactGroupId;
	}

	public String getLibArtifactId() {
		return libArtifactId;
	}

	public void setLibArtifactId(String libArtifactId) {
		this.libArtifactId = libArtifactId;
	}

	public String getLibArtifactVersion() {
		return libArtifactVersion;
	}

	public void setLibArtifactVersion(String libArtifactVersion) {
		this.libArtifactVersion = libArtifactVersion;
	}

	public String getLibArtifactClassifier() {
		return libArtifactClassifier;
	}

	public void setLibArtifactClassifier(String libArtifactClassifier) {
		this.libArtifactClassifier = libArtifactClassifier;
	}

	public String getLibTrackingAttr() {
		return libTrackingAttr;
	}

	public void setLibTrackingAttr(String libTrackingAttr) {
		this.libTrackingAttr = libTrackingAttr;
	}
}
