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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.automation.packages.client.AutomationPackageClientException;
import step.automation.packages.client.RemoteAutomationPackageClientImpl;
import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.client.executions.RemoteExecutionManager;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.artefacts.reports.aggregated.AggregatedReportView;
import step.core.execution.model.AutomationPackageExecutionParameters;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionMode;
import step.core.plans.PlanFilter;
import step.core.plans.filters.*;
import step.core.plans.runner.PlanRunnerResult;
import step.core.repositories.RepositoryObjectReference;
import step.repositories.ArtifactRepositoryConstants;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public abstract class AbstractExecuteAutomationPackageTool extends AbstractCliTool {

    private static final Logger logger = LoggerFactory.getLogger(AbstractExecuteAutomationPackageTool.class);

    private final String stepProjectName;
    private final String userId;
    private final String authToken;

    private final Map<String, String> executionParameters;
    private final Integer executionResultTimeoutS;
    private final Boolean waitForExecution;
    private final Boolean ensureExecutionSuccess;
    private final Boolean printAggregatedReport;

    private final String includePlans;
    private final String excludePlans;
    private final Boolean wrapIntoTestSet;
    private final Integer numberOfThreads;
    private final MavenArtifactIdentifier mavenArtifactIdentifier;
    private final String includeCategories;
    private final String excludeCategories;

    public AbstractExecuteAutomationPackageTool(String url, String stepProjectName,
                                                String userId, String authToken,
                                                Map<String, String> executionParameters,
                                                Integer executionResultTimeoutS, Boolean waitForExecution,
                                                Boolean ensureExecutionSuccess, Boolean printAggregatedReport, String includePlans,
                                                String excludePlans, String includeCategories, String excludeCategories,
                                                Boolean wrapIntoTestSet, Integer numberOfThreads,
                                                MavenArtifactIdentifier mavenArtifactIdentifier) {
        super(url);
        this.stepProjectName = stepProjectName;
        this.userId = userId;
        this.authToken = authToken;
        this.executionParameters = executionParameters;
        this.executionResultTimeoutS = executionResultTimeoutS;
        this.waitForExecution = waitForExecution;
        this.ensureExecutionSuccess = ensureExecutionSuccess;
        this.printAggregatedReport = printAggregatedReport;
        this.includePlans = includePlans;
        this.excludePlans = excludePlans;
        this.includeCategories = includeCategories;
        this.excludeCategories = excludeCategories;
        this.wrapIntoTestSet = wrapIntoTestSet;
        this.numberOfThreads = numberOfThreads;
        this.mavenArtifactIdentifier = mavenArtifactIdentifier;
    }

    public static String getExecutionTreeAsString(PlanRunnerResult res) {
        String executionTree;
        Writer w = new StringWriter();
        try {
            res.printTree(w, true, true);
            executionTree = w.toString();
        } catch (IOException e) {
            logger.error("Error while writing execution tree: {}", w);
            executionTree = "Error while writing tree. See logs for details.";
        }
        return executionTree;
    }

    public void execute() throws StepCliExecutionException {
        executePackageOnStep();
    }

    protected void executePackageOnStep() throws StepCliExecutionException {
        try (RemoteAutomationPackageClientImpl automationPackageClient = createRemoteAutomationPackageClient();
             RemoteExecutionManager remoteExecutionManager = createRemoteExecutionManager()) {
            File automationPackageFile = null;

            // if group id and artifact id are specified, this means, that don't want to send the artifact (binary) to the controller,
            if (useLocalArtifact()) {
                automationPackageFile = getAutomationPackageFile();
            }

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

    protected boolean useLocalArtifact() {
        return mavenArtifactIdentifier == null;
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
                } else {
                    //for now print aggregated report
                    if (getPrintAggregatedReport()) {
                        AggregatedReportView aggregatedReportView = remoteExecutionManager.getAggregatedReportView(endedExecution.getId().toString());
                        logInfo("Aggregated report:\n" + aggregatedReportView.toString(), null);
                    }
                    if (!isStatusSuccess(endedExecution)) {
                        executionFailureCount++;
                        String errorSummary = remoteExecutionManager.getFuture(id).getErrorSummary();
                        logError("Execution " + executionToString(id, endedExecution) + " failed. Result status was " + endedExecution.getResult() + ". Error summary: " + errorSummary, null);
                    } else {
                        logInfo("Execution " + executionToString(id, endedExecution) + " succeeded. Result status was " + endedExecution.getResult(), null);
                    }
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

        executionParameters.setPlanFilter(getPlanFilters(includePlans, excludePlans, includeCategories, excludeCategories));
        executionParameters.setWrapIntoTestSet(wrapIntoTestSet);
        executionParameters.setNumberOfThreads(numberOfThreads);

        if (mavenArtifactIdentifier != null) {
            Map<String, String> repositoryParameters = new HashMap<>();
            repositoryParameters.put(ArtifactRepositoryConstants.ARTIFACT_PARAM_ARTIFACT_ID, mavenArtifactIdentifier.getArtifactId());
            repositoryParameters.put(ArtifactRepositoryConstants.ARTIFACT_PARAM_GROUP_ID, mavenArtifactIdentifier.getGroupId());
            repositoryParameters.put(ArtifactRepositoryConstants.ARTIFACT_PARAM_VERSION, mavenArtifactIdentifier.getVersion());
            repositoryParameters.put(ArtifactRepositoryConstants.ARTIFACT_PARAM_CLASSIFIER, mavenArtifactIdentifier.getClassifier());
            executionParameters.setOriginalRepositoryObject(new RepositoryObjectReference(ArtifactRepositoryConstants.MAVEN_REPO_ID, repositoryParameters));
        }

        return executionParameters;
    }

     public static PlanFilter getPlanFilters(String includePlans, String excludePlans, String includeCategories, String excludeCategories) {
        List<PlanFilter> multiFilter = new ArrayList<>();
        if (includePlans != null) {
            multiFilter.add(new PlanByIncludedNamesFilter(parseList(includePlans)));
        }
        if (excludePlans != null) {
            multiFilter.add(new PlanByExcludedNamesFilter(parseList(excludePlans)));
        }
        if (includeCategories != null) {
            multiFilter.add(new PlanByIncludedCategoriesFilter(parseList(includeCategories)));
        }
        if (excludeCategories != null) {
            multiFilter.add(new PlanByExcludedCategoriesFilter(parseList(excludeCategories)));
        }
        return new PlanMultiFilter(multiFilter);
    }

    private static List<String> parseList(String string) {
        return (string != null && !string.isBlank()) ? Arrays.stream(string.split(",")).collect(Collectors.toList()) : new ArrayList<>();
    }

    public String getStepProjectName() {
        return stepProjectName;
    }

    public String getUserId() {
        return userId;
    }

    public String getAuthToken() {
        return authToken;
    }

    public Map<String, String> getExecutionParameters() {
        return executionParameters;
    }

    public Integer getExecutionResultTimeoutS() {
        return executionResultTimeoutS;
    }

    public Boolean getWaitForExecution() {
        return waitForExecution;
    }


    public Boolean getEnsureExecutionSuccess() {
        return ensureExecutionSuccess;
    }


    public Boolean getPrintAggregatedReport() {
        return printAggregatedReport;
    }

    public String getIncludePlans() {
        return includePlans;
    }

    public String getExcludePlans() {
        return excludePlans;
    }

    public String getIncludeCategories() {
        return includeCategories;
    }

    public String getExcludeCategories() {
        return excludeCategories;
    }

    public MavenArtifactIdentifier getMavenArtifactIdentifier() {
        return mavenArtifactIdentifier;
    }
}
