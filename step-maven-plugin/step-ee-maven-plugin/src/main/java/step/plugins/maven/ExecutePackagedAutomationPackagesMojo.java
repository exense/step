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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import step.automation.packages.client.RemoteAutomationPackageClientImpl;
import step.automation.packages.execution.AutomationPackageExecutionParameters;
import step.client.executions.RemoteExecutionManager;
import step.controller.multitenancy.client.MultitenancyClient;
import step.controller.multitenancy.client.RemoteMultitenancyClientImpl;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionMode;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Mojo(name = "exec-packaged-automation-package")
public class ExecutePackagedAutomationPackagesMojo extends AbstractStepPluginMojo {

    @Parameter(property = "step.step-project-name", required = true)
    private String stepProjectName;

    @Parameter(property = "step-run-auto-packages.user-id", required = false)
    private String userId;

    @Parameter(property = "step.auth-token", required = false)
    private String authToken;

    @Parameter(property = "step-run-auto-packages.group-id", required = true, defaultValue = "${project.groupId}")
    private String groupId;
    @Parameter(property = "step-run-auto-packages.artifact-id", required = true, defaultValue = "${project.artifactId}")
    private String artifactId;
    @Parameter(property = "step-run-auto-packages.artifact-version", required = true, defaultValue = "${project.version}")
    private String artifactVersion;
    @Parameter(property = "step-run-auto-packages.artifact-classifier", required = false)
    private String artifactClassifier;

    @Parameter(property = "step-run-auto-packages.execution-parameters", required = false)
    private Map<String, String> executionParameters;
    @Parameter(property = "step-run-auto-packages.exec-result-timeout-s", defaultValue = "3600")
    private Integer executionResultTimeoutS;
    @Parameter(property = "step-run-auto-packages.wait-for-exec", defaultValue = "true")
    private Boolean waitForExecution;
    @Parameter(property = "step-run-auto-packages.ensure-exec-success", defaultValue = "true")
    private Boolean ensureExecutionSuccess;

    @Parameter(property = "step-run-auto-packages.include-plans")
    private String includePlans;
    @Parameter(property = "step-run-auto-packages.exclude-plans")
    private String excludePlans;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (getStepProjectName() != null && !getStepProjectName().isEmpty()) {
            getLog().info("Step project name: " + getStepProjectName());

            new TenantSwitcher() {
                @Override
                protected MultitenancyClient createClient() {
                    return createMultitenancyClient();
                }
            }.switchTenant(getStepProjectName(), getLog());
        }

        executePackageOnStep();
    }

    protected void executePackageOnStep() throws MojoExecutionException {
        try (RemoteAutomationPackageClientImpl automationPackageClient = createRemoteAutomationPackageClient();
             RemoteExecutionManager remoteExecutionManager = createRemoteExecutionManager()) {
            File automationPackageFile = getAutomationPackageFile();
            AutomationPackageExecutionParameters executionParameters = prepareExecutionParameters();

            List<String> executionIds = automationPackageClient.executeAutomationPackage(automationPackageFile, executionParameters);
            String baseMessage = "Execution has been started in Step (" + getUrl() + "): " + executionIds;

            if (getWaitForExecution()) {
                getLog().info(baseMessage + ". Waiting on results...");
                waitForExecutionFinish(remoteExecutionManager, executionIds);
            } else {
                getLog().info(baseMessage + ". Waiting on results is disabled.");
            }
        } catch (Exception ex) {
            throw logAndThrow("Unable to run execution in Step (" + getUrl() + ")", ex);
        }
    }

    protected void waitForExecutionFinish(RemoteExecutionManager remoteExecutionManager, List<String> executionIds) throws MojoExecutionException {
        getLog().info("Waiting for execution result from Step (" + getUrl() + ")...");

        // run the execution and wait until it is finished
        try {
            List<Execution> endedExecutions = remoteExecutionManager.waitForTermination(executionIds, getExecutionResultTimeoutS() * 1000);
            boolean error = false;
            for (String id : executionIds) {
                Execution endedExecution = endedExecutions.stream().filter(e -> e.getId().toString().equals(id)).findFirst().orElse(null);
                if (endedExecution == null) {
                    error = true;
                    getLog().error("Unknown result status for execution " + id);
                } else if (getEnsureExecutionSuccess() && !endedExecution.getImportResult().isSuccessful()) {
                    error = true;
                    getLog().error("The execution result is NOT OK for execution " + id + ". The following error(s) occurred during import " +
                            String.join(";", endedExecution.getImportResult().getErrors()));
                } else {
                    getLog().info("The execution result is OK. Final status is " + endedExecution.getResult());
                }
            }
            if (error) {
                throw new MojoExecutionException("Execution failure");
            }
        } catch (TimeoutException | InterruptedException ex) {
            throw logAndThrow("The success execution result is not received from Step in " + getExecutionResultTimeoutS() + "seconds", ex);
        }
    }

    protected File getAutomationPackageFile() throws MojoExecutionException {
        Artifact applicableArtifact = getProjectArtifact(getArtifactClassifier(), getGroupId(), getArtifactId(), getArtifactVersion());

        if (applicableArtifact != null) {
            return applicableArtifact.getFile();
        } else {
            throw logAndThrow("Unable to resolve automation package file " + artifactToString(getGroupId(), getArtifactId(), getArtifactClassifier(), getArtifactVersion()));
        }
    }

    protected MultitenancyClient createMultitenancyClient() {
        return new RemoteMultitenancyClientImpl(getControllerCredentials());
    }

    protected RemoteAutomationPackageClientImpl createRemoteAutomationPackageClient() {
        return new RemoteAutomationPackageClientImpl(getControllerCredentials());
    }

    protected RemoteExecutionManager createRemoteExecutionManager() {
        return new RemoteExecutionManager(getControllerCredentials());
    }

    protected AutomationPackageExecutionParameters prepareExecutionParameters() {
        AutomationPackageExecutionParameters executionParameters = new AutomationPackageExecutionParameters();
        executionParameters.setMode(ExecutionMode.RUN);
        executionParameters.setCustomParameters(getExecutionParameters());
        executionParameters.setUserID(getUserId());

        // TODO: set plan filters?
        return executionParameters;
    }

    public String getStepProjectName() {
        return stepProjectName;
    }

    public void setStepProjectName(String stepProjectName) {
        this.stepProjectName = stepProjectName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
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

    public String getIncludePlans() {
        return includePlans;
    }

    public void setIncludePlans(String includePlans) {
        this.includePlans = includePlans;
    }

    public String getExcludePlans() {
        return excludePlans;
    }

    public void setExcludePlans(String excludePlans) {
        this.excludePlans = excludePlans;
    }

}
