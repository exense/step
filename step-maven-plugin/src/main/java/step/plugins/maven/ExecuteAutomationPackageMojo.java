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
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import step.automation.packages.client.AutomationPackageClientException;
import step.automation.packages.client.RemoteAutomationPackageClientImpl;
import step.automation.packages.execution.AutomationPackageExecutionParameters;
import step.client.credentials.ControllerCredentials;
import step.client.executions.RemoteExecutionManager;
import step.controller.multitenancy.client.MultitenancyClient;
import step.controller.multitenancy.client.RemoteMultitenancyClientImpl;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionMode;
import step.core.plans.PlanFilter;
import step.core.plans.filters.PlanByExcludedNamesFilter;
import step.core.plans.filters.PlanByIncludedNamesFilter;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Mojo(name = "execute-automation-package")
public class ExecuteAutomationPackageMojo extends AbstractStepPluginMojo {

    @Parameter(property = "step.step-project-name", required = false)
    private String stepProjectName;
    @Parameter(property = "step-execute-auto-packages.user-id", required = false)
    private String userId;
    @Parameter(property = "step.auth-token", required = false)
    private String authToken;

    @Parameter(property = "step-execute-auto-packages.group-id", required = true, defaultValue = "${project.groupId}")
    private String groupId;
    @Parameter(property = "step-execute-auto-packages.artifact-id", required = true, defaultValue = "${project.artifactId}")
    private String artifactId;
    @Parameter(property = "step-execute-auto-packages.artifact-version", required = true, defaultValue = "${project.version}")
    private String artifactVersion;
    @Parameter(property = "step-execute-auto-packages.artifact-classifier", required = false)
    private String artifactClassifier;

    @Parameter(property = "step-execute-auto-packages.execution-parameters", required = false)
    private Map<String, String> executionParameters;
    @Parameter(property = "step-execute-auto-packages.exec-result-timeout-s", defaultValue = "3600")
    private Integer executionResultTimeoutS;
    @Parameter(property = "step-execute-auto-packages.wait-for-exec", defaultValue = "true")
    private Boolean waitForExecution;
    @Parameter(property = "step-execute-auto-packages.ensure-exec-success", defaultValue = "true")
    private Boolean ensureExecutionSuccess;

    @Parameter(property = "step-execute-auto-packages.include-plans")
    private String includePlans;
    @Parameter(property = "step-execute-auto-packages.exclude-plans")
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

            List<String> executionIds;
            try {
                executionIds = automationPackageClient.executeAutomationPackage(automationPackageFile, executionParameters);
            } catch (AutomationPackageClientException e) {
                throw logAndThrow("Error while executing automation package: " + e.getMessage());
            }
            if(executionIds != null) {
                getLog().info("Execution(s) started in Step:");
                for (String executionId : executionIds) {
                    Execution executionInfo = remoteExecutionManager.get(executionId);
                    getLog().info("- " + executionToString(executionId, executionInfo));
                }

                if (getWaitForExecution()) {
                    getLog().info("Waiting for execution(s) to complete...");
                    waitForExecutionFinish(remoteExecutionManager, executionIds);
                } else {
                    getLog().info("waitForExecution set to 'false'. Not waiting for executions to complete.");
                }
            } else {
                throw logAndThrow("Unexpected response from Step. No execution Id returned. Please check the controller logs.");
            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception ex) {
            throw logAndThrow("Unexpected error while executing automation package", ex);
        }
    }

    protected void waitForExecutionFinish(RemoteExecutionManager remoteExecutionManager, List<String> executionIds) throws MojoExecutionException {
        // run the execution and wait until it is finished
        try {
            List<Execution> endedExecutions = remoteExecutionManager.waitForTermination(executionIds, getExecutionResultTimeoutS() * 1000);
            int executionFailureCount = 0;
            for (String id : executionIds) {
                Execution endedExecution = endedExecutions.stream().filter(e -> e.getId().toString().equals(id)).findFirst().orElse(null);
                Log log = getLog();
                if (endedExecution == null) {
                    executionFailureCount++;
                    log.error("Unknown result status for execution " + executionToString(id, null));
                } else if (!endedExecution.getImportResult().isSuccessful()) {
                    executionFailureCount++;
                    String errorMessage = "Error(s) while importing plan for execution " + executionToString(id, endedExecution);
                    List<String> errors = endedExecution.getImportResult().getErrors();
                    if (errors != null) {
                        errorMessage += ": " + String.join(";", errors);
                    }
                    log.error(errorMessage);
                } else if (!isStatusSuccess(endedExecution)) {
                    executionFailureCount++;
                    String errorSummary = remoteExecutionManager.getFuture(id).getErrorSummary();
                    log.error("Execution " + executionToString(id, endedExecution) + " failed. Result status was " + endedExecution.getResult() + ". Error summary: " + errorSummary);
                } else {
                    log.info("Execution " + executionToString(id, endedExecution) + " succeeded. Result status was " + endedExecution.getResult());
                }
            }
            if (executionFailureCount > 0 && getEnsureExecutionSuccess()) {
                int executionsCount = executionIds.size();
                throw logAndThrow(executionFailureCount + "/" + executionsCount + " execution(s) failed. See " + getUrl() + "#/root/executions/list");
            }
        } catch (TimeoutException | InterruptedException ex) {
            throw logAndThrow("Timeout after " + getExecutionResultTimeoutS() + " seconds while waiting for executions to complete", ex);
        } catch (MojoExecutionException e) {
            // Rethrow MojoExecutionException
            throw e;
        } catch (Exception e) {
            throw logAndThrow("Unexpected error while executing automation package", e);
        }
    }

    private boolean isStatusSuccess(Execution ex){
        Set<ReportNodeStatus> okStatus = Set.of(ReportNodeStatus.PASSED, ReportNodeStatus.SKIPPED, ReportNodeStatus.NORUN);
        return okStatus.contains(ex.getResult());
    }

    private String executionToString(String id, Execution ex) {
        if (ex != null) {
            return String.format("'%s' (%s)", ex.getDescription(), getUrl() + "#/root/executions/" + ex.getId().toString());
        } else {
            return id;
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

    @Override
    protected ControllerCredentials getControllerCredentials() {
        String authToken = getAuthToken();
        return new ControllerCredentials(getUrl(), authToken == null || authToken.isEmpty() ? null : authToken);
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

    protected AutomationPackageExecutionParameters prepareExecutionParameters() throws MojoExecutionException {
        AutomationPackageExecutionParameters executionParameters = new AutomationPackageExecutionParameters();
        executionParameters.setMode(ExecutionMode.RUN);
        executionParameters.setCustomParameters(getExecutionParameters());
        executionParameters.setUserID(getUserId());

        PlanFilter planFilter = null;
        if (getIncludePlans() != null && !getIncludePlans().isEmpty()) {
            planFilter = new PlanByIncludedNamesFilter(Arrays.stream(getIncludePlans().split(",")).collect(Collectors.toList()));
        }
        if (getExcludePlans() != null && !getExcludePlans().isEmpty()) {
            if (planFilter != null) {
                throw new MojoExecutionException("Plan filter configuration is ambiguous. Please use one of the following parameters: includePlans, excludePlans");
            }
            planFilter = new PlanByExcludedNamesFilter(Arrays.stream(getExcludePlans().split(",")).collect(Collectors.toList()));
        }
        if (planFilter != null) {
            executionParameters.setPlanFilter(planFilter);
        }

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
