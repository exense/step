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
package step.cli;

import step.automation.packages.client.AutomationPackageClientException;
import step.automation.packages.client.RemoteAutomationPackageClientImpl;
import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.client.executions.RemoteExecutionManager;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.model.AutomationPackageExecutionParameters;
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

public abstract class AbstractExecuteAutomationPackageTool extends AbstractCliTool {

    private String stepProjectName;
    private String userId;
    private String authToken;

    private String groupId;
    private String artifactId;
    private String artifactVersion;
    private String artifactClassifier;

    private Map<String, String> executionParameters;
    private Integer executionResultTimeoutS;
    private Boolean waitForExecution;
    private Boolean ensureExecutionSuccess;

    private String includePlans;
    private String excludePlans;

    public AbstractExecuteAutomationPackageTool(String url, String stepProjectName,
                                                String userId, String authToken,
                                                String groupId, String artifactGroupId,
                                                String artifactVersion, String artifactClassifier,
                                                Map<String, String> executionParameters,
                                                Integer executionResultTimeoutS, Boolean waitForExecution,
                                                Boolean ensureExecutionSuccess, String includePlans,
                                                String excludePlans) {
        super(url);
        this.stepProjectName = stepProjectName;
        this.userId = userId;
        this.authToken = authToken;
        this.groupId = groupId;
        this.artifactId = artifactGroupId;
        this.artifactVersion = artifactVersion;
        this.artifactClassifier = artifactClassifier;
        this.executionParameters = executionParameters;
        this.executionResultTimeoutS = executionResultTimeoutS;
        this.waitForExecution = waitForExecution;
        this.ensureExecutionSuccess = ensureExecutionSuccess;
        this.includePlans = includePlans;
        this.excludePlans = excludePlans;
    }

    public void execute() throws StepCliExecutionException {
        executePackageOnStep();
    }

    protected void executePackageOnStep() throws StepCliExecutionException {
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
            if (executionIds != null) {
                logInfo("Execution(s) started in Step:", null);
                for (String executionId : executionIds) {
                    Execution executionInfo = remoteExecutionManager.get(executionId);
                    logInfo("- " + executionToString(executionId, executionInfo), null);
                }

                if (getWaitForExecution()) {
                    logInfo("Waiting for execution(s) to complete...", null);
                    waitForExecutionFinish(remoteExecutionManager, executionIds);
                } else {
                    logInfo("waitForExecution set to 'false'. Not waiting for executions to complete.", null);
                }
            } else {
                throw logAndThrow("Unexpected response from Step. No execution Id returned. Please check the controller logs.");
            }
        } catch (StepCliExecutionException e) {
            throw e;
        } catch (Exception ex) {
            throw logAndThrow("Unexpected error while executing automation package", ex);
        }
    }

    protected void waitForExecutionFinish(RemoteExecutionManager remoteExecutionManager, List<String> executionIds) throws StepCliExecutionException {
        // run the execution and wait until it is finished
        try {
            List<Execution> endedExecutions = remoteExecutionManager.waitForTermination(executionIds, getExecutionResultTimeoutS() * 1000);
            int executionFailureCount = 0;
            for (String id : executionIds) {
                Execution endedExecution = endedExecutions.stream().filter(e -> e.getId().toString().equals(id)).findFirst().orElse(null);
                if (endedExecution == null) {
                    executionFailureCount++;
                    logError("Unknown result status for execution " + executionToString(id, null), null);
                } else if (!endedExecution.getImportResult().isSuccessful()) {
                    executionFailureCount++;
                    String errorMessage = "Error(s) while importing plan for execution " + executionToString(id, endedExecution);
                    List<String> errors = endedExecution.getImportResult().getErrors();
                    if (errors != null) {
                        errorMessage += ": " + String.join(";", errors);
                    }
                    logError(errorMessage, null);
                } else if (!isStatusSuccess(endedExecution)) {
                    executionFailureCount++;
                    String errorSummary = remoteExecutionManager.getFuture(id).getErrorSummary();
                    logError("Execution " + executionToString(id, endedExecution) + " failed. Result status was " + endedExecution.getResult() + ". Error summary: " + errorSummary, null);
                } else {
                    logInfo("Execution " + executionToString(id, endedExecution) + " succeeded. Result status was " + endedExecution.getResult(), null);
                }
            }
            if (executionFailureCount > 0 && getEnsureExecutionSuccess()) {
                int executionsCount = executionIds.size();
                throw logAndThrow(executionFailureCount + "/" + executionsCount + " execution(s) failed. See " + getUrl() + "#/executions/list");
            }
        } catch (TimeoutException | InterruptedException ex) {
            throw logAndThrow("Timeout after " + getExecutionResultTimeoutS() + " seconds while waiting for executions to complete", ex);
        } catch (StepCliExecutionException e) {
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
            return String.format("'%s' (%s)", ex.getDescription(), getUrl() + "#/executions/" + ex.getId().toString());
        } else {
            return id;
        }
    }

    protected abstract File getAutomationPackageFile() throws StepCliExecutionException;

    @Override
    protected ControllerCredentials getControllerCredentials() {
        String authToken = getAuthToken();
        return new ControllerCredentials(getUrl(), authToken == null || authToken.isEmpty() ? null : authToken);
    }

    protected RemoteAutomationPackageClientImpl createRemoteAutomationPackageClient() {
        RemoteAutomationPackageClientImpl client = new RemoteAutomationPackageClientImpl(getControllerCredentials());
        addProjectHeaderToRemoteClient(client);
        return client;
    }

    protected RemoteExecutionManager createRemoteExecutionManager() {
        RemoteExecutionManager remoteExecutionManager = new RemoteExecutionManager(getControllerCredentials());
        addProjectHeaderToRemoteClient(remoteExecutionManager);
        return remoteExecutionManager;
    }

    private void addProjectHeaderToRemoteClient(AbstractRemoteClient remoteClient) {
        addProjectHeaderToRemoteClient(getStepProjectName(), remoteClient);
    }

    protected AutomationPackageExecutionParameters prepareExecutionParameters() throws StepCliExecutionException {
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
                throw new StepCliExecutionException("Plan filter configuration is ambiguous. Please use one of the following parameters: includePlans, excludePlans");
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
